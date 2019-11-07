package no.ssb.dc.application.controller;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import no.ssb.dc.application.Controller;

import java.util.Map;
import java.util.NavigableMap;

public class DispatchController implements HttpHandler {

    private final String corsAllowOrigin;
    private final String corsAllowHeaders;
    private final boolean corsAllowOriginTest;
    private final int undertowPort;
    private final NavigableMap<String, Controller> controllers;

    public DispatchController(String corsAllowOrigin, String corsAllowHeaders, boolean corsAllowOriginTest, int undertowPort, NavigableMap<String, Controller> controllers) {
        this.controllers = controllers;
        this.corsAllowOrigin = corsAllowOrigin;
        this.corsAllowHeaders = corsAllowHeaders;
        this.corsAllowOriginTest = corsAllowOriginTest;
        this.undertowPort = undertowPort;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String requestPath = exchange.getRequestPath();

//        // NOTE: CORSController cannot be shared across requests or threads
//        CORSController cors = new CORSController(corsAllowOrigin, corsAllowHeaders, corsAllowOriginTest, undertowPort);
//
//        cors.handleRequest(exchange);
//
//        if (requestPath.trim().length() <= 1 && !cors.isOptionsRequest()) {
//            exchange.setStatusCode(404);
//            return;
//        }
//
//        if (cors.isBadRequest()) {
//            return;
//        }
//
//        if (cors.isOptionsRequest()) {
//            cors.doOptions();
//            return;
//        }
//
//        cors.handleValidRequest();

        if (requestPath.startsWith("/ping")) {
            new PingController().handleRequest(exchange);
            return;
        }

        String path = requestPath.substring(1).split("/")[0];
        Map.Entry<String, Controller> entry = controllers.floorEntry("/"+path);
        if (entry != null) {
            Controller controller = entry.getValue();
            controller.handleRequest(exchange);
            return;
        }

        exchange.setStatusCode(400);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        String namespace = requestPath.substring(1, Math.max(requestPath.substring(1).indexOf("/") + 1, requestPath.length()));
        exchange.getResponseSender().send("Unsupported namespace: \"" + namespace + "\"");
    }
}
