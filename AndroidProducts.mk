PRODUCT_MAKEFILES := $(LOCAL_DIR)/full_spyder.mk
ifeq ($(TARGET_PRODUCT),aokp_spyder)
    PRODUCT_MAKEFILES += $(LOCAL_DIR)/aokp.mk
endif
ifeq ($(TARGET_PRODUCT),cna_spyder)
    PRODUCT_MAKEFILES += $(LOCAL_DIR)/cna.mk
endif
ifeq ($(TARGET_PRODUCT),killrom_spyder)
    PRODUCT_MAKEFILES += $(LOCAL_DIR)/killrom.mk
endif
