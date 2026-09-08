/*
 * Copyright 2020 The Android Open Source Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <android/hardware/bluetooth/audio/2.1/IBluetoothAudioProvidersFactory.h>

#include "A2dpOffloadAudioProvider.h"
#include "A2dpSoftwareAudioProvider.h"
#include "BluetoothAudioProvider.h"
#include "HearingAidAudioProvider.h"
#include "LeAudioAudioProvider.h"
#include "LeAudioOffloadAudioProvider.h"

namespace android {
namespace hardware {
namespace bluetooth {
namespace audio {
namespace V2_1 {
namespace implementation {

class BluetoothAudioProvidersFactory : public IBluetoothAudioProvidersFactory {
 public:
  BluetoothAudioProvidersFactory() {}

  Return<void> openProvider(const V2_0::SessionType sessionType,
                            openProvider_cb _hidl_cb) override;

  Return<void> getProviderCapabilities(
      const V2_0::SessionType sessionType,
      getProviderCapabilities_cb _hidl_cb) override;

  Return<void> openProvider_2_1(const SessionType sessionType,
                                openProvider_2_1_cb _hidl_cb) override;

  Return<void> getProviderCapabilities_2_1(
      const SessionType sessionType,
      getProviderCapabilities_2_1_cb _hidl_cb) override;

 private:
  static A2dpSoftwareAudioProvider a2dp_software_provider_instance_;
  static A2dpOffloadAudioProvider a2dp_offload_provider_instance_;
  static HearingAidAudioProvider hearing_aid_provider_instance_;
  static LeAudioOutputAudioProvider leaudio_output_provider_instance_;
  static LeAudioInputAudioProvider leaudio_input_provider_instance_;
};

extern "C" IBluetoothAudioProvidersFactory*
HIDL_FETCH_IBluetoothAudioProvidersFactory(const char* name);

}  // namespace implementation
}  // namespace V2_1
}  // namespace audio
}  // namespace bluetooth
}  // namespace hardware
}  // namespace android
