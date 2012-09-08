PRODUCT_MAKEFILES := $(LOCAL_DIR)/full_spyder.mk
ifeq ($(TARGET_PRODUCT),aokp_spyder)
    PRODUCT_MAKEFILES += $(LOCAL_DIR)/aokp.mk
endif
