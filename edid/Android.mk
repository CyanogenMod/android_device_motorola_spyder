LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    lib/edid_parser.c

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libedid

include $(BUILD_SHARED_LIBRARY)

# ====================
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    cmd/parse_hdmi_edid.c

LOCAL_SHARED_LIBRARIES:= \
    libutils \
    libedid

LOCAL_MODULE:= parse_hdmi_edid
LOCAL_MODULE_TAGS:= optional

include $(BUILD_EXECUTABLE)
