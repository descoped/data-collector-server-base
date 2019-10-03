package no.ssb.dc.application;

import io.undertow.server.HttpHandler;

public interface Controller extends HttpHandler {

    String contextPath();

}
