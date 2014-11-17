# Copyright (C) 2013 The CyanogenMod Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# This is the product configuration for a full spyder
#
$(call inherit-product, device/motorola/omap4-common/common.mk)

DEVICE_FOLDER := device/motorola/spyder

# Device overlay
    DEVICE_PACKAGE_OVERLAYS += $(DEVICE_FOLDER)/overlay

# Hardware HALs
PRODUCT_PACKAGES += \
    power.spyder \
    camera.spyder \
    lights.spyder \
    hwcomposer.spyder

# Modem
PRODUCT_PACKAGES += \
    nc \
    Stk \
    libreference-cdma-sms \
    wrigleyd

# Kexec files
PRODUCT_COPY_FILES += \
    $(DEVICE_FOLDER)/init.target.rc:root/init.target.rc \
    $(DEVICE_FOLDER)/kexec/devtree:system/etc/kexec/devtree \
    $(OUT)/ramdisk.img:system/etc/kexec/ramdisk.img \
    $(OUT)/kernel:system/etc/kexec/kernel

# Prebuilts
PRODUCT_COPY_FILES += \
    $(DEVICE_FOLDER)/media_profiles.xml:system/etc/media_profiles.xml \
    $(DEVICE_FOLDER)/prebuilt/usr/keylayout/cy8c201xx.kl:system/usr/keylayout/cy8c201xx.kl

$(call inherit-product, frameworks/native/build/phone-xhdpi-1024-dalvik-heap.mk)
$(call inherit-product, vendor/motorola/spyder/spyder-vendor.mk)
$(call inherit-product, vendor/motorola/omap4-common/omap4-common-imgtec-vendor-blobs.mk)
$(call inherit-product, vendor/motorola/omap4-common/omap4-common-lte-vendor-blobs.mk)
