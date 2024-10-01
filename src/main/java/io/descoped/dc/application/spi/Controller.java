package io.descoped.dc.application.spi;

import io.descoped.dc.api.http.Request;
import io.undertow.server.HttpHandler;

import java.util.Set;

public interface Controller extends HttpHandler {

    String contextPath();

    Set<Request.Method> allowedMethods();

}
