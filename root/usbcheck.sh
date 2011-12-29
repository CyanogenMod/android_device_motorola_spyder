#!/system/bin/sh

USBCONN=/sys/devices/platform/cpcap_usb_connected

if [[ -d "${USBCONN}" ]] ; then
    #Restart with root hacked adbd
    echo msc_adb > /dev/usb_device_mode
fi

