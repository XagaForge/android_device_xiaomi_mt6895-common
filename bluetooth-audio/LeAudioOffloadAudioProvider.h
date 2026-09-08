/*
 * Copyright 2021 The Android Open Source Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <android/hardware/bluetooth/audio/2.1/types.h>

#include "BluetoothAudioProvider.h"

namespace android {
namespace hardware {
namespace bluetooth {
namespace audio {
namespace V2_1 {
namespace implementation {

class LeAudioOffloadAudioProvider : public BluetoothAudioProvider {
 public:
  LeAudioOffloadAudioProvider();

  bool isValid(const SessionType& sessionType) override;
  bool isValid(const V2_0::SessionType& sessionType) override;

  Return<void> startSession_2_1(const sp<V2_0::IBluetoothAudioPort>& hostIf,
                                const AudioConfiguration& audioConfig,
                                startSession_cb _hidl_cb) override;

 private:
  Return<void> onSessionReady(startSession_cb _hidl_cb) override;
};

class LeAudioOffloadOutputAudioProvider : public LeAudioOffloadAudioProvider {
 public:
  LeAudioOffloadOutputAudioProvider();
};

class LeAudioOffloadInputAudioProvider : public LeAudioOffloadAudioProvider {
 public:
  LeAudioOffloadInputAudioProvider();
};

}  // namespace implementation
}  // namespace V2_1
}  // namespace audio
}  // namespace bluetooth
}  // namespace hardware
}  // namespace android
