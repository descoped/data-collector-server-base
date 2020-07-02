package no.ssb.dc.application.controller;

import io.undertow.server.HttpServerExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathHandler {

    private final HttpServerExchange exchange;
    private final Map<String, String> parameters;
    private final Map<String, Object> outcome = new ConcurrentHashMap<>();

    public PathHandler(HttpServerExchange exchange, Map<String, String> parameters) {
        this.exchange = exchange;
        this.parameters = parameters;
    }

    public HttpServerExchange exchange() {
        return exchange;
    }

    public Map<String, String> parameters() {
        return parameters;
    }

    public Map<String, Object> outcome() {
        return outcome;
    }
}
