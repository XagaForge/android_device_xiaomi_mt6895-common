/*
 * Copyright 2020 HIMSA II K/S - www.himsa.com. Represented by EHIMA -
 * www.ehima.com
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <android/hardware/bluetooth/audio/2.1/types.h>
#include <fmq/MessageQueue.h>
#include <hidl/MQDescriptor.h>

#include "BluetoothAudioProvider.h"

namespace android {
namespace hardware {
namespace bluetooth {
namespace audio {
namespace V2_1 {
namespace implementation {

using ::android::hardware::kSynchronizedReadWrite;
using ::android::hardware::MessageQueue;

using DataMQ = MessageQueue<uint8_t, kSynchronizedReadWrite>;

class LeAudioAudioProvider : public BluetoothAudioProvider {
 public:
  LeAudioAudioProvider();

  bool isValid(const SessionType& sessionType) override;
  bool isValid(const V2_0::SessionType& sessionType) override;

  Return<void> startSession_2_1(const sp<V2_0::IBluetoothAudioPort>& hostIf,
                                const AudioConfiguration& audioConfig,
                                startSession_cb _hidl_cb) override;

 private:
  /** queue for software encodec/decoded audio data */
  std::unique_ptr<DataMQ> mDataMQ;

  Return<void> onSessionReady(startSession_cb _hidl_cb) override;
};

class LeAudioOutputAudioProvider : public LeAudioAudioProvider {
 public:
  LeAudioOutputAudioProvider();
};

class LeAudioInputAudioProvider : public LeAudioAudioProvider {
 public:
  LeAudioInputAudioProvider();
};

}  // namespace implementation
}  // namespace V2_1
}  // namespace audio
}  // namespace bluetooth
}  // namespace hardware
}  // namespace android
