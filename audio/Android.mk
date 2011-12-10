ifeq ($(BOARD_USES_AUDIO_LEGACY),true)

LOCAL_PATH := $(call my-dir)

# output for libaudio intermediates
LIBAUDIO_INTERMEDIATES_PREREQS := $(PRODUCT_OUT)/obj/lib

# prerequisites for building audio
file := $(LIBAUDIO_INTERMEDIATES_PREREQS)/libaudio.so
$(file) : device/motorola/$(TARGET_BOOTLOADER_BOARD_NAME)/audio/libaudio.so
	@echo "Copy libaudio.so -> $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) cp -a device/motorola/$(TARGET_BOOTLOADER_BOARD_NAME)/audio/libaudio.so $@

file := $(LIBAUDIO_INTERMEDIATES_PREREQS)/liba2dp.so
$(file) : device/motorola/$(TARGET_BOOTLOADER_BOARD_NAME)/audio/liba2dp.so
	@echo "Copy liba2dp.so -> $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) cp -a device/motorola/$(TARGET_BOOTLOADER_BOARD_NAME)/audio/liba2dp.so $@

file := $(LIBAUDIO_INTERMEDIATES_PREREQS)/libaudio_ext.so
$(file) : device/motorola/$(TARGET_BOOTLOADER_BOARD_NAME)/audio/libaudio_ext.so
	@echo "Copy libaudio_ext.so -> $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) cp -a device/motorola/$(TARGET_BOOTLOADER_BOARD_NAME)/audio/libaudio_ext.so $@

file := $(LIBAUDIO_INTERMEDIATES_PREREQS)/libasound.so
$(file) : device/motorola/$(TARGET_BOOTLOADER_BOARD_NAME)/audio/libasound.so
	@echo "Copy libasound.so -> $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) cp -a device/motorola/$(TARGET_BOOTLOADER_BOARD_NAME)/audio/libasound.so $@

file := $(LIBAUDIO_INTERMEDIATES_PREREQS)/libaudiopolicy.so
$(file) : device/motorola/targa/$(TARGET_BOOTLOADER_BOARD_NAME)/libaudiopolicy.so
	@echo "Copy libaudiopolicy.so -> $@"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) cp -a device/motorola/$(TARGET_BOOTLOADER_BOARD_NAME)/audio/libaudiopolicy.so $@

include $(all-subdir-makefiles)

endif # BOARD_USES_AUDIO_LEGACY
