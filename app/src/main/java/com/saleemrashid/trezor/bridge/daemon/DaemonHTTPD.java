package com.saleemrashid.trezor.bridge.daemon;

import android.hardware.usb.UsbDevice;

import com.saleemrashid.trezor.bridge.daemon.handlers.IndexRoute;

import java.util.Map;

import fi.iki.elonen.router.RouterNanoHTTPD;

public class DaemonHTTPD extends RouterNanoHTTPD {
    private final Map<String, UsbDevice> mDevices;

    public DaemonHTTPD(String hostname, int port, final Map<String, UsbDevice> devices) {
        super(hostname, port);

        addMappings();

        mDevices = devices;
    }

    @Override
    public void addMappings() {
        setNotImplementedHandler(NotImplementedHandler.class);
        setNotFoundHandler(Error404UriHandler.class);

        addRoute(IndexRoute.URL, IndexRoute.class);
    }
}
