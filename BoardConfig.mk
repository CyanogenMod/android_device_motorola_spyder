# inherit from common
-include device/motorola/omap4-common/BoardConfigCommon.mk

# inherit from the proprietary version
-include vendor/motorola/spyder/BoardConfigVendor.mk

# Processor
TARGET_BOOTLOADER_BOARD_NAME := spyder

# Kernel
BOARD_KERNEL_CMDLINE := omap_wdt.timer_margin=60 oops=panic console=/dev/null rw mem=1023M@0x80000000 vram=11140K omapfb.vram=0:8256K,1:4K,2:2880K init=/init ip=off mmcparts=mmcblk1:p7(pds),p8(utags),p14(boot),p15(recovery),p16(cdrom),p17(misc),p18(cid),p19(kpanic),p20(system),p21(cache),p22(preinstall),p23(webtop),p24(userdata),p25(emstorage) mot_sst=1 androidboot.bootloader=0x0A74
BOARD_KERNEL_BASE := 0x80000000
BOARD_PAGE_SIZE := 0x4096
