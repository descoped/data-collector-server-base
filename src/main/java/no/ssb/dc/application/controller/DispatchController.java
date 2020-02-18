package no.ssb.dc.application.controller;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import no.ssb.dc.application.spi.Controller;

import java.util.Map;
import java.util.NavigableMap;

public class DispatchController implements HttpHandler {

    private final NavigableMap<String, Controller> controllers;

    public DispatchController(NavigableMap<String, Controller> controllers) {
        this.controllers = controllers;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String requestPath = exchange.getRequestPath();

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
        String namespace = requestPath.substring(1, Math.max("/".indexOf(requestPath.substring(1)) + 1, requestPath.length()));
        exchange.getResponseSender().send("Unsupported namespace: \"" + namespace + "\"");
    }
}
