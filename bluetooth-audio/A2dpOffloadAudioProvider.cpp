/*
 * Copyright 2020 The Android Open Source Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "BTAudioProviderA2dpOffload"

#include "A2dpOffloadAudioProvider.h"

#include <android-base/logging.h>
#include <android-base/properties.h>
#include <dlfcn.h>
#include <fmq/MessageQueue.h>
#include <hidl/MQDescriptor.h>
#include <string>

#include <vendor/mediatek/hardware/bluetooth/audio/2.1/IBluetoothAudioPort.h>
#include <vendor/mediatek/hardware/bluetooth/audio/2.2/types.h>

#include "BluetoothAudioSessionReport_2_1.h"
#include "BluetoothAudioSupportedCodecsDB_2_1.h"

namespace android {
namespace hardware {
namespace bluetooth {
namespace audio {
namespace V2_1 {
namespace implementation {

using ::android::bluetooth::audio::BluetoothAudioSessionReport_2_1;
using ::android::hardware::kSynchronizedReadWrite;
using ::android::hardware::MessageQueue;
using ::android::hardware::Void;
using ::android::hardware::bluetooth::audio::V2_0::AudioConfiguration;

using MtkAudioPort = ::vendor::mediatek::hardware::bluetooth::audio::V2_1::IBluetoothAudioPort;
using MtkAudioConfiguration = ::vendor::mediatek::hardware::bluetooth::audio::V2_2::AudioConfiguration;

using SetParamFn = int (*)(void* hw, const sp<MtkAudioPort>& hostIf,
                           const MtkAudioConfiguration& audioConfig,
                           bool bEnable, int btType);
using SetStatusFn = void (*)(void* hw, int status);

using DataMQ = MessageQueue<uint8_t, kSynchronizedReadWrite>;

static void* sMtkAudioHw = nullptr;
static SetParamFn sSetBtOffloadParam = nullptr;
static SetStatusFn sSetA2dpSuspendStatus = nullptr;

static bool initAudioHal() {
  if (sMtkAudioHw != nullptr) {
    return true;
  }

  std::string platform = android::base::GetProperty("ro.board.platform", "");
  if (platform.empty()) {
    return false;
  }

  std::string path = "/vendor/lib64/hw/audio.primary." + platform + ".so";
  void* handle = dlopen(path.c_str(), RTLD_NOW);
  if (handle == nullptr) {
    LOG(ERROR) << __func__ << ": failed to open " << path << ": " << dlerror();
    return false;
  }

  auto createMTKAudioHardware = reinterpret_cast<void* (*)()>(
      dlsym(handle, "createMTKAudioHardware"));
  if (createMTKAudioHardware == nullptr ||
      (sMtkAudioHw = createMTKAudioHardware()) == nullptr) {
    return false;
  }

  sSetBtOffloadParam = reinterpret_cast<SetParamFn>(dlsym(
      handle,
      "_ZN7android17AudioALSAHardware29setBluetoothAudioOffloadParamERKNS_2spIN6"
      "vendor8mediatek8hardware9bluetooth5audio4V2_119IBluetoothAudioPortEEERKNS6"
      "_4V2_218AudioConfigurationEbi"));

  sSetA2dpSuspendStatus = reinterpret_cast<SetStatusFn>(
      dlsym(handle, "_ZN7android17AudioALSAHardware20setA2dpSuspendStatusEi"));

  return true;
}

static bool setBtOffloadParam(const sp<IBluetoothAudioPort>& hostIf,
                              const V2_1::AudioConfiguration& audioConfig,
                              bool bEnable, int sessionType) {
  if (!initAudioHal() || sSetBtOffloadParam == nullptr) {
    return false;
  }

  MtkAudioConfiguration mtkConfig = {};
  if (bEnable) {
    mtkConfig.codecConfig(
        reinterpret_cast<const ::vendor::mediatek::hardware::bluetooth::audio::
                             V2_1::CodecConfiguration&>(
            audioConfig.codecConfig()));
  }

  return sSetBtOffloadParam(sMtkAudioHw,
                            reinterpret_cast<const sp<MtkAudioPort>&>(hostIf),
                            mtkConfig, bEnable, sessionType) == 0;
}

static void setA2dpSuspendStatus(int status) {
  if (!initAudioHal() || sSetA2dpSuspendStatus == nullptr) {
    return;
  }

  sSetA2dpSuspendStatus(sMtkAudioHw, status);
}

A2dpOffloadAudioProvider::A2dpOffloadAudioProvider()
    : BluetoothAudioProvider() {
  session_type_ = SessionType::A2DP_HARDWARE_OFFLOAD_DATAPATH;
}

bool A2dpOffloadAudioProvider::isValid(const V2_0::SessionType& sessionType) {
  return isValid(static_cast<SessionType>(sessionType));
}

bool A2dpOffloadAudioProvider::isValid(const SessionType& sessionType) {
  return (sessionType == session_type_);
}

Return<void> A2dpOffloadAudioProvider::startSession(
    const sp<IBluetoothAudioPort>& hostIf,
    const AudioConfiguration& audioConfig, startSession_cb _hidl_cb) {
  /**
   * Initialize the audio platform if audioConfiguration is supported.
   * Save the IBluetoothAudioPort interface, so that it can be used
   * later to send stream control commands to the HAL client, based on
   * interaction with Audio framework.
   */
  if (audioConfig.getDiscriminator() !=
      AudioConfiguration::hidl_discriminator::codecConfig) {
    LOG(WARNING) << __func__
                 << " - Invalid Audio Configuration=" << toString(audioConfig);
    _hidl_cb(BluetoothAudioStatus::UNSUPPORTED_CODEC_CONFIGURATION,
             DataMQ::Descriptor());
    return Void();
  } else if (!android::bluetooth::audio::IsOffloadCodecConfigurationValid(
                 session_type_, audioConfig.codecConfig())) {
    _hidl_cb(BluetoothAudioStatus::UNSUPPORTED_CODEC_CONFIGURATION,
             DataMQ::Descriptor());
    return Void();
  }

  return BluetoothAudioProvider::startSession(hostIf, audioConfig, _hidl_cb);
}

Return<void> A2dpOffloadAudioProvider::startSession_2_1(
    const sp<IBluetoothAudioPort>& hostIf,
    const V2_1::AudioConfiguration& audioConfig, startSession_cb _hidl_cb) {
  if (hostIf == nullptr ||
      audioConfig.getDiscriminator() !=
          V2_1::AudioConfiguration::hidl_discriminator::codecConfig ||
      !setBtOffloadParam(hostIf, audioConfig, true,
                         static_cast<int>(session_type_))) {
    _hidl_cb(BluetoothAudioStatus::FAILURE, DataMQ::Descriptor());
    return Void();
  }

  return BluetoothAudioProvider::startSession_2_1(hostIf, audioConfig,
                                                  _hidl_cb);
}

Return<void> A2dpOffloadAudioProvider::streamStarted(
    BluetoothAudioStatus status) {
  setA2dpSuspendStatus(static_cast<int>(status));

  return BluetoothAudioProvider::streamStarted(status);
}

Return<void> A2dpOffloadAudioProvider::streamSuspended(
    BluetoothAudioStatus status) {
  setA2dpSuspendStatus(static_cast<int>(status) + 3);

  return BluetoothAudioProvider::streamSuspended(status);
}

Return<void> A2dpOffloadAudioProvider::endSession() {
  setBtOffloadParam(nullptr, {}, false, static_cast<int>(session_type_));

  return BluetoothAudioProvider::endSession();
}

Return<void> A2dpOffloadAudioProvider::onSessionReady(
    startSession_cb _hidl_cb) {
  BluetoothAudioSessionReport_2_1::OnSessionStarted(session_type_, stack_iface_,
                                                    nullptr, audio_config_);
  _hidl_cb(BluetoothAudioStatus::SUCCESS, DataMQ::Descriptor());
  return Void();
}

}  // namespace implementation
}  // namespace V2_1
}  // namespace audio
}  // namespace bluetooth
}  // namespace hardware
}  // namespace android
