package com.saleemrashid.trezor.bridge.daemon.routes;

import com.saleemrashid.trezor.bridge.daemon.DaemonHTTPD;
import com.saleemrashid.trezor.bridge.daemon.handlers.JsonHandler;
import com.saleemrashid.trezor.bridge.daemon.types.IndexGson;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

public class IndexRoute extends JsonHandler {
    public static final String URL = "/";

    @Override
    public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        final DaemonHTTPD daemon = uriResource.initParameter(DaemonHTTPD.class);

        final IndexGson response;
        if (daemon.getConfiguration() == null) {
            response = new IndexGson(DaemonHTTPD.VERSION, false, null);
        } else {
            response = new IndexGson(DaemonHTTPD.VERSION, true, daemon.getConfiguration().getValidUntil());
        }

        return createJsonResponse(NanoHTTPD.Response.Status.OK, response);
    }
}
