package com.saleemrashid.trezor.bridge.daemon.handlers;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;


public class NotFoundHandler extends JsonHandler {
    /* Emulates official daemon */
    private static final String ERROR_MESSAGE = "Not Found";

    @Override
    public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", ERROR_MESSAGE);
    }
}
