/*
 * Copyright 2020 The Android Open Source Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include "BluetoothAudioProvider.h"

namespace android {
namespace hardware {
namespace bluetooth {
namespace audio {
namespace V2_1 {
namespace implementation {

class A2dpOffloadAudioProvider : public BluetoothAudioProvider {
 public:
  A2dpOffloadAudioProvider();

  bool isValid(const SessionType& sessionType) override;
  bool isValid(const V2_0::SessionType& sessionType) override;

  Return<void> startSession(const sp<V2_0::IBluetoothAudioPort>& hostIf,
                            const V2_0::AudioConfiguration& audioConfig,
                            startSession_cb _hidl_cb) override;
  Return<void> startSession_2_1(const sp<V2_0::IBluetoothAudioPort>& hostIf,
                                const V2_1::AudioConfiguration& audioConfig,
                                startSession_cb _hidl_cb) override;
  Return<void> streamStarted(BluetoothAudioStatus status) override;
  Return<void> streamSuspended(BluetoothAudioStatus status) override;
  Return<void> endSession() override;

 private:
  Return<void> onSessionReady(startSession_cb _hidl_cb) override;
};

}  // namespace implementation
}  // namespace V2_1
}  // namespace audio
}  // namespace bluetooth
}  // namespace hardware
}  // namespace android
