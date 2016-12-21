package com.saleemrashid.trezor.bridge.daemon.types;

public class IndexGson {
    public String version;
    public boolean configured;
    public Integer validUntil;

    public IndexGson(String version, boolean configured, Integer validUntil) {
        this.version = version;
        this.configured = configured;
        this.validUntil = validUntil;
    }
}
