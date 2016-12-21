package com.saleemrashid.trezor.bridge.daemon.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

public abstract class JsonHandler implements RouterNanoHTTPD.UriResponder {
    public static final String MIME_TYPE_JSON = "application/json";
    public static final String MIME_TYPE_PLAIN = "text/plain";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    protected Gson getGson() {
        return GSON;
    }

    protected NanoHTTPD.Response createJsonResponse(final NanoHTTPD.Response.IStatus status, final Object json) {
        return NanoHTTPD.newFixedLengthResponse(status, MIME_TYPE_JSON, getGson().toJson(json));
    }

    protected NanoHTTPD.Response createPlainResponse(final NanoHTTPD.Response.IStatus status, final String message) {
        return NanoHTTPD.newFixedLengthResponse(status, MIME_TYPE_PLAIN, message);
    }

    @Override
    public NanoHTTPD.Response put(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

    @Override
    public NanoHTTPD.Response post(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

    @Override
    public NanoHTTPD.Response delete(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

    @Override
    public NanoHTTPD.Response other(String method, RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }
}
