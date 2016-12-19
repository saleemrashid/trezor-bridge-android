package com.saleemrashid.trezor.bridge;

import android.hardware.usb.UsbDevice;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class DaemonHTTPD extends NanoHTTPD {
    private final Map<String, UsbDevice> mDevices;

    public DaemonHTTPD(String hostname, int port, final Map<String, UsbDevice> devices) {
        super(hostname, port);

        mDevices = devices;
    }
}
