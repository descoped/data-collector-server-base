package no.ssb.dc.application.controller;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.net.HttpURLConnection;

public class PingController implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.setStatusCode(HttpURLConnection.HTTP_OK);
    }
}
