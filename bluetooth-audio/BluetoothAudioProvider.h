/*
 * Copyright 2020 The Android Open Source Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <android/hardware/bluetooth/audio/2.1/IBluetoothAudioProvider.h>

namespace android {
namespace hardware {
namespace bluetooth {
namespace audio {
namespace V2_1 {
namespace implementation {

using ::android::sp;
using ::android::hardware::bluetooth::audio::V2_0::IBluetoothAudioPort;

using BluetoothAudioStatus =
    ::android::hardware::bluetooth::audio::V2_0::Status;

class BluetoothAudioDeathRecipient;

class BluetoothAudioProvider : public IBluetoothAudioProvider {
 public:
  BluetoothAudioProvider();
  ~BluetoothAudioProvider() = default;

  virtual bool isValid(const SessionType& sessionType) = 0;
  virtual bool isValid(const V2_0::SessionType& sessionType) = 0;

  Return<void> startSession(const sp<IBluetoothAudioPort>& hostIf,
                            const V2_0::AudioConfiguration& audioConfig,
                            startSession_cb _hidl_cb) override;
  Return<void> startSession_2_1(const sp<IBluetoothAudioPort>& hostIf,
                                const AudioConfiguration& audioConfig,
                                startSession_cb _hidl_cb) override;
  Return<void> streamStarted(BluetoothAudioStatus status) override;
  Return<void> streamSuspended(BluetoothAudioStatus status) override;
  Return<void> endSession() override;

 protected:
  sp<BluetoothAudioDeathRecipient> death_recipient_;

  SessionType session_type_;
  AudioConfiguration audio_config_;
  sp<V2_0::IBluetoothAudioPort> stack_iface_;

  virtual Return<void> onSessionReady(startSession_cb _hidl_cb) = 0;
};

class BluetoothAudioDeathRecipient : public hidl_death_recipient {
 public:
  BluetoothAudioDeathRecipient(const sp<BluetoothAudioProvider> provider)
      : provider_(provider) {}

  virtual void serviceDied(
      uint64_t cookie,
      const wp<::android::hidl::base::V1_0::IBase>& who) override;

 private:
  sp<BluetoothAudioProvider> provider_;
};

}  // namespace implementation
}  // namespace V2_1
}  // namespace audio
}  // namespace bluetooth
}  // namespace hardware
}  // namespace android
