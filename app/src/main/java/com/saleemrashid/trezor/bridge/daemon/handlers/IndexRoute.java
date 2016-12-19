package com.saleemrashid.trezor.bridge.daemon.handlers;

import com.saleemrashid.trezor.bridge.BuildConfig;
import com.saleemrashid.trezor.bridge.daemon.DaemonHTTPD;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class IndexRoute extends JsonHandler {
    @Override
    public Object getJson() {
        /* TODO: Use real data */
        return new IndexGson(BuildConfig.TREZOR_BRIDGE_VERSION, false, null);
    }

    @Override
    public NanoHTTPD.Response.IStatus getStatus() {
        return Status.OK;
    }

    private static class IndexGson {
        public String version;
        public Boolean configured;
        public Integer validUntil;

        public IndexGson(String version, Boolean configured, Integer validUntil) {
            this.version = version;
            this.configured = configured;
            this.validUntil = validUntil;
        }
    }
}
