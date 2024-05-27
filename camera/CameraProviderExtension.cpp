/*
 * Copyright (C) 2026 The LibreMobileOS Foundation
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "CameraProviderExtension.h"

#include <atomic>
#include <fstream>
#include <unistd.h>

static const std::string kTorchBrightnessPath =
        "/sys/class/flashlight_core/flashlight/torchbrightness";

constexpr int32_t kTorchDefaultStrengthLevel = 250;
// The flashlight core sysfs node accepts 380, but the KTD2687 torch register
// overflows past 377 because the driver converts combined current to a 7-bit
// per-LED value. Advertising 377 avoids wrapping the register to 0 at 378-380.
constexpr int32_t kTorchMaxStrengthLevel = 377;

static std::atomic<bool> gTorchEnabled{false};
static std::atomic<int32_t> gTorchStrengthLevel{kTorchDefaultStrengthLevel};

template <typename T>
static bool writeValue(const std::string& path, const T& value) {
    std::ofstream file(path);
    if (!file.is_open()) {
        return false;
    }

    file << value;
    return file.good();
}

template <typename T>
static bool readValue(const std::string& path, T* out) {
    std::ifstream file(path);
    if (!file.is_open()) {
        return false;
    }

    T value;
    file >> value;
    if (file.fail()) {
        return false;
    }

    *out = value;
    return true;
}

bool supportsTorchStrengthControlExt() {
    return access(kTorchBrightnessPath.c_str(), R_OK | W_OK) == 0;
}

int32_t getTorchDefaultStrengthLevelExt() {
    return kTorchDefaultStrengthLevel;
}

int32_t getTorchMaxStrengthLevelExt() {
    return kTorchMaxStrengthLevel;
}

int32_t getTorchStrengthLevelExt() {
    if (!gTorchEnabled.load()) {
        return gTorchStrengthLevel.load();
    }

    int32_t strength = gTorchStrengthLevel.load();
    if (readValue(kTorchBrightnessPath, &strength)) {
        gTorchStrengthLevel.store(strength);
    }

    return gTorchStrengthLevel.load();
}

void setTorchStrengthLevelExt(int32_t torchStrength, bool enabled) {
    if (!supportsTorchStrengthControlExt()) {
        return;
    }

    const int32_t hardwareStrength = enabled ? torchStrength : 0;
    if (writeValue(kTorchBrightnessPath, hardwareStrength)) {
        gTorchEnabled.store(enabled);
        gTorchStrengthLevel.store(enabled ? torchStrength : kTorchDefaultStrengthLevel);
    }
}
