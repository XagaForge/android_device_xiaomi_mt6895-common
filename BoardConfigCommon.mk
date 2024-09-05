#
# Copyright (C) 2023 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

COMMON_PATH := device/xiaomi/mt6895-common

# A/B
AB_OTA_UPDATER := true

AB_OTA_PARTITIONS += \
    system \
    system_ext \
    product \
    vendor \
    vendor_dlkm \
    odm \
    odm_dlkm \
    boot \
    vendor_boot \
    vbmeta \
    vbmeta_system \
    vbmeta_vendor

# APEX
DEXPREOPT_GENERATE_APEX_IMAGE := true

# Architecture
TARGET_ARCH := arm64
TARGET_ARCH_VARIANT := armv8-2a-dotprod
TARGET_CPU_ABI := arm64-v8a
TARGET_CPU_ABI2 :=
TARGET_CPU_VARIANT := generic
TARGET_CPU_VARIANT_RUNTIME := cortex-a76

# Boot Image
BOARD_KERNEL_BASE         := 0x3fff8000
BOARD_KERNEL_OFFSET       := 0x00008000
BOARD_KERNEL_PAGESIZE     := 4096
BOARD_RAMDISK_OFFSET      := 0x26f08000
BOARD_KERNEL_TAGS_OFFSET  := 0x07c88000
BOARD_DTB_OFFSET          := 0x07c88000
BOARD_BOOT_HEADER_VERSION := 4

BOARD_INCLUDE_DTB_IN_BOOTIMG := true

BOARD_RAMDISK_USE_LZ4 := true
BOARD_USES_GENERIC_KERNEL_IMAGE := true
BOARD_MOVE_GSI_AVB_KEYS_TO_VENDOR_BOOT := true

# base and pagesize are usually ignored when BOARD_USES_GENERIC_KERNEL_IMAGE is true,
# but our bootloader is fussy and cares about these offsets being set correctly.
BOARD_MKBOOTIMG_ARGS := --base $(BOARD_KERNEL_BASE)
BOARD_MKBOOTIMG_ARGS += --kernel_offset $(BOARD_KERNEL_OFFSET)
BOARD_MKBOOTIMG_ARGS += --pagesize $(BOARD_KERNEL_PAGESIZE)
BOARD_MKBOOTIMG_ARGS += --ramdisk_offset $(BOARD_RAMDISK_OFFSET)
BOARD_MKBOOTIMG_ARGS += --tags_offset $(BOARD_KERNEL_TAGS_OFFSET)
BOARD_MKBOOTIMG_ARGS += --dtb_offset $(BOARD_DTB_OFFSET)
BOARD_MKBOOTIMG_ARGS += --header_version $(BOARD_BOOT_HEADER_VERSION)

BOARD_KERNEL_CMDLINE := bootopt=64S3,32N2,64N2 bootconfig

# Bootloader
TARGET_NO_BOOTLOADER := true

# HIDL
DEVICE_MANIFEST_FILE += $(COMMON_PATH)/manifest.xml
ifeq ($(TARGET_HAS_UDFPS),true)
DEVICE_MANIFEST_FILE += $(COMMON_PATH)/manifest_udfps.xml
endif
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += \
    $(COMMON_PATH)/framework_compatibility_matrix.xml \
    hardware/xiaomi/vintf/xiaomi_framework_compatibility_matrix.xml \
    vendor/lineage/config/device_framework_matrix.xml

# Kernel
TARGET_KERNEL_ARCH := arm64
TARGET_KERNEL_HEADER_ARCH := arm64
TARGET_KERNEL_SOURCE := kernel/xiaomi/mt6895
TARGET_KERNEL_CLANG_VERSION := r416183b
TARGET_KERNEL_CLANG_PATH := $(abspath .)/prebuilts/clang/kernel/$(HOST_PREBUILT_TAG)/clang-$(TARGET_KERNEL_CLANG_VERSION)
TARGET_KERNEL_CONFIG := \
	gki_defconfig \
	vendor/xiaomi_mt6895.config \
	vendor/$(PRODUCT_DEVICE).config

TARGET_KERNEL_DTB := \
    vendor/mediatek/mt6895.dtb

BOARD_KERNEL_IMAGE_NAME := Image.gz

# Lineage Health
TARGET_HEALTH_CHARGING_CONTROL_SUPPORTS_BYPASS := false

# Partitions
BOARD_FLASH_BLOCK_SIZE := 262144 # 4096 * 64 (pagesize)
BOARD_BOOTIMAGE_PARTITION_SIZE := 67108864
BOARD_VENDOR_BOOTIMAGE_PARTITION_SIZE := 67108864
BOARD_DTBOIMG_PARTITION_SIZE := 33554432
BOARD_USES_METADATA_PARTITION := true

ifneq ($(WITH_GMS),true)
ifeq ($(PRODUCT_SYSTEM_PARTITIONS_FILE_SYSTEM_TYPE),ext4)
BOARD_PRODUCTIMAGE_EXTFS_INODE_COUNT := -1
BOARD_PRODUCTIMAGE_PARTITION_RESERVED_SIZE := 1887436800
BOARD_SYSTEMIMAGE_EXTFS_INODE_COUNT := -1
BOARD_SYSTEMIMAGE_PARTITION_RESERVED_SIZE := 1887436800
BOARD_SYSTEM_EXTIMAGE_EXTFS_INODE_COUNT := -1
BOARD_SYSTEM_EXTIMAGE_PARTITION_RESERVED_SIZE := 104857600
endif
endif

# Partitions - Dynamic
BOARD_SUPER_PARTITION_SIZE := 9126805504
BOARD_SUPER_PARTITION_GROUPS := main
BOARD_MAIN_SIZE := 9122611200 # BOARD_SUPER_PARTITION_SIZE - 4MB

BOARD_MAIN_PARTITION_LIST := \
    system \
    system_ext \
    product \
    vendor \
    odm \
    vendor_dlkm \
    odm_dlkm

BOARD_ODMIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_ODM_DLKMIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_VENDORIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_VENDOR_DLKMIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_PRODUCTIMAGE_FILE_SYSTEM_TYPE := $(PRODUCT_SYSTEM_PARTITIONS_FILE_SYSTEM_TYPE)
BOARD_SYSTEMIMAGE_FILE_SYSTEM_TYPE := $(PRODUCT_SYSTEM_PARTITIONS_FILE_SYSTEM_TYPE)
BOARD_SYSTEM_EXTIMAGE_FILE_SYSTEM_TYPE := $(PRODUCT_SYSTEM_PARTITIONS_FILE_SYSTEM_TYPE)

TARGET_COPY_OUT_ODM := odm
TARGET_COPY_OUT_ODM_DLKM := odm_dlkm
TARGET_COPY_OUT_PRODUCT := product
TARGET_COPY_OUT_SYSTEM_EXT := system_ext
TARGET_COPY_OUT_VENDOR := vendor
TARGET_COPY_OUT_VENDOR_DLKM := vendor_dlkm

# Platform
TARGET_BOARD_PLATFORM := mt6895
BOARD_HAS_MTK_HARDWARE := true
BOARD_VENDOR := xiaomi
BOARD_TEE_VARIANT ?= beanpod

# Power
TARGET_POWER_LIBPERFMGR_MODE_EXTENSION_LIB := //$(COMMON_PATH):libperfmgr-ext-xiaomi

# Properties
TARGET_SYSTEM_PROP += $(COMMON_PATH)/system.prop
TARGET_VENDOR_PROP += $(COMMON_PATH)/vendor.prop
TARGET_VENDOR_PROP += $(COMMON_PATH)/vendor-$(BOARD_TEE_VARIANT).prop

# Recovery
TARGET_RECOVERY_FSTAB := $(COMMON_PATH)/init/fstab.mt6895
TARGET_RECOVERY_PIXEL_FORMAT := "RGBX_8888"
TARGET_USERIMAGES_USE_F2FS := true
BOARD_MOVE_RECOVERY_RESOURCES_TO_VENDOR_BOOT := true

# RIL
ENABLE_VENDOR_RIL_SERVICE := true

# Sepolicy
include device/mediatek/sepolicy_vndr/SEPolicy.mk
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(COMMON_PATH)/sepolicy/private
SYSTEM_EXT_PUBLIC_SEPOLICY_DIRS += $(COMMON_PATH)/sepolicy/public
BOARD_VENDOR_SEPOLICY_DIRS += $(COMMON_PATH)/sepolicy/vendor

# Verified Boot
BOARD_AVB_ENABLE := true
BOARD_AVB_MAKE_VBMETA_IMAGE_ARGS += --flags 3
BOARD_AVB_RECOVERY_KEY_PATH := external/avb/test/data/testkey_rsa4096.pem
BOARD_AVB_RECOVERY_ALGORITHM := SHA256_RSA4096
BOARD_AVB_RECOVERY_ROLLBACK_INDEX := 1
BOARD_AVB_RECOVERY_ROLLBACK_INDEX_LOCATION := 1

BOARD_AVB_VBMETA_SYSTEM := product system_ext system
BOARD_AVB_VBMETA_SYSTEM_KEY_PATH := external/avb/test/data/testkey_rsa2048.pem
BOARD_AVB_VBMETA_SYSTEM_ALGORITHM := SHA256_RSA2048
BOARD_AVB_VBMETA_SYSTEM_ROLLBACK_INDEX := 1
BOARD_AVB_VBMETA_SYSTEM_ROLLBACK_INDEX_LOCATION := 2

BOARD_AVB_VBMETA_VENDOR := vendor odm
BOARD_AVB_VBMETA_VENDOR_KEY_PATH := external/avb/test/data/testkey_rsa2048.pem
BOARD_AVB_VBMETA_VENDOR_ALGORITHM := SHA256_RSA2048
BOARD_AVB_VBMETA_VENDOR_ROLLBACK_INDEX := 1
BOARD_AVB_VBMETA_VENDOR_ROLLBACK_INDEX_LOCATION := 3

# VNDK
BOARD_VNDK_VERSION := current

# Wi-Fi
WPA_SUPPLICANT_VERSION := VER_0_8_X
BOARD_WPA_SUPPLICANT_DRIVER := NL80211
BOARD_HOSTAPD_DRIVER := NL80211
WIFI_DRIVER_FW_PATH_PARAM := "/dev/wmtWifi"
WIFI_DRIVER_FW_PATH_STA := "STA"
WIFI_DRIVER_FW_PATH_AP := "AP"
WIFI_DRIVER_FW_PATH_P2P := "P2P"
WIFI_DRIVER_STATE_CTRL_PARAM := "/dev/wmtWifi"
WIFI_DRIVER_STATE_ON := "1"
WIFI_DRIVER_STATE_OFF := "0"
WIFI_HIDL_UNIFIED_SUPPLICANT_SERVICE_RC_ENTRY := true
WIFI_HIDL_FEATURE_DUAL_INTERFACE := true

# Inherit the proprietary files
include vendor/xiaomi/mt6895-common/BoardConfigVendor.mk
