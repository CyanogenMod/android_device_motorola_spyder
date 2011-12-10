# Inherit device configuration for VZW Droid RAZR.
$(call inherit-product, device/motorola/spyder/device_spyder.mk)

# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Inherit some common CM stuff.
#$(call inherit-product, vendor/cm/config/gsm.mk)

#
# Setup device specific product configuration.
#
PRODUCT_NAME := cm_spyder
PRODUCT_BRAND := verizon
PRODUCT_DEVICE := spyder
PRODUCT_MODEL := XT912
PRODUCT_MANUFACTURER := Motorola
PRODUCT_SFX := vzw

# Release name and versioning
PRODUCT_RELEASE_NAME := DROID_RAZR

UTC_DATE := $(shell date +%s)
DATE := $(shell date +%Y%m%d)

PRODUCT_BUILD_PROP_OVERRIDES += \
   BUILD_ID=6.5.1-73_DHD-11_TA-3 \
   PRODUCT_NAME=${PRODUCT_DEVICE}_${PRODUCT_SFX} \
   BUILD_NUMBER=${DATE} \
   TARGET_DEVICE=cdma_spyder \
   BUILD_DISPLAY_ID=6.5.1-73_DHD-11_TA-3 \
   BUILD_FINGERPRINT=verizon/spyder_vzw/cdma_spyder:4.0.1/6.5.1-73_DHD-11_TA-3/${BUILD_NUMBER}:user/release-keys \
   PRIVATE_BUILD_DESC="cdma_spyder-user 4.0.1 6.5.1-73_DHD-11_TA-3 "${BUILD_NUMBER}" release-keys" \
   PRODUCT_BRAND=verizon \
   BUILD_UTC_DATE= \
   TARGET_BUILD_TYPE=user \
   BUILD_VERSION_TAGS=release-keys \
   USER=hashcode \
   BUILD_HOST=unn-hashcode \
   PRODUCT_DEFAULT_LANGUAGE=en \
   PRODUCT_DEFAULT_REGION=US \

# Extra Droid_RAZR overlay
#PRODUCT_PACKAGE_OVERLAYS += vendor/cm/overlay/spyder
