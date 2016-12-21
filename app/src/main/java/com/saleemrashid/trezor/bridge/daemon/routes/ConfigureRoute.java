package com.saleemrashid.trezor.bridge.daemon.routes;

import com.saleemrashid.trezor.bridge.daemon.handlers.JsonHandler;
import com.saleemrashid.trezor.bridge.daemon.handlers.NotFoundHandler;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

public class ConfigureRoute extends JsonHandler {
    public static final String URL = "/configure";

    @Override
    public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return new NotFoundHandler().get(uriResource, urlParams, session);
    }

    @Override
    public NanoHTTPD.Response post(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        /* TODO: Load signed configuration, handle errors */
        return createPlainResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "Configuration not implemented");
    }
}
