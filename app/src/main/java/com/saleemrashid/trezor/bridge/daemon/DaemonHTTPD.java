package com.saleemrashid.trezor.bridge.daemon;

import android.hardware.usb.UsbDevice;

import com.saleemrashid.trezor.bridge.BuildConfig;
import com.saleemrashid.trezor.bridge.daemon.routes.ConfigureRoute;
import com.saleemrashid.trezor.bridge.daemon.routes.IndexRoute;
import com.saleemrashid.trezor.bridge.daemon.handlers.NotFoundHandler;
import com.satoshilabs.trezor.protobuf.TrezorConfig;

import java.util.Map;

import fi.iki.elonen.router.RouterNanoHTTPD;

public class DaemonHTTPD extends RouterNanoHTTPD {
    public static final String VERSION = BuildConfig.TREZOR_BRIDGE_VERSION;

    private final Map<String, UsbDevice> mDevices;

    private TrezorConfig.Configuration mConfiguration = null;

    public DaemonHTTPD(String hostname, int port, final Map<String, UsbDevice> devices) {
        super(hostname, port);

        addMappings();

        mDevices = devices;
    }

    @Override
    public void addMappings() {
        setNotFoundHandler(NotFoundHandler.class);

        addRoute(IndexRoute.URL, IndexRoute.class, this);
        addRoute(ConfigureRoute.URL, ConfigureRoute.class, this);
    }

    public TrezorConfig.Configuration getConfiguration() {
        return mConfiguration;
    }

}
