ifeq ($(BOARD_USES_AUDIO_LEGACY),true)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= AudioPolicyManager.cpp

LOCAL_SHARED_LIBRARIES:= \
    liblog \
    libcutils \
    libutils \
    libmedia \
    libm \
    libaudiopolicy \
    libc

LOCAL_STATIC_LIBRARIES := \
    libmedia_helper

LOCAL_WHOLE_STATIC_LIBRARIES:= \
    libaudiopolicy_legacy

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/hw
LOCAL_MODULE:= audio_policy.$(TARGET_BOOTLOADER_BOARD_NAME)
LOCAL_MODULE_TAGS := optional

ifeq ($(BOARD_HAVE_BLUETOOTH),true)
  LOCAL_CFLAGS += -DWITH_A2DP
endif

include $(BUILD_SHARED_LIBRARY)

endif

