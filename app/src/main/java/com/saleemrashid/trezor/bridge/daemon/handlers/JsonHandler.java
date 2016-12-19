package com.saleemrashid.trezor.bridge.daemon.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fi.iki.elonen.router.RouterNanoHTTPD;

public abstract class JsonHandler extends RouterNanoHTTPD.DefaultHandler {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    protected Gson getGson() {
        return GSON;
    }

    public abstract Object getJson();

    @Override
    public String getText() {
        return getGson().toJson(getJson());
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }
}
