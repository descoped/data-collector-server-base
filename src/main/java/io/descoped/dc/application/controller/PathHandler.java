package io.descoped.dc.application.controller;

import io.descoped.dc.api.http.HttpStatus;
import io.undertow.server.HttpServerExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathHandler {

    private final HttpServerExchange exchange;
    private final Map<String, String> parameters;
    private final Map<String, Object> outcome = new ConcurrentHashMap<>();
    private HttpStatus statusCode;

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

    public HttpStatus statusCode() {
        return this.statusCode;
    }

    public void statusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }
}
