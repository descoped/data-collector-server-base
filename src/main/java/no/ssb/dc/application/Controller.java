package no.ssb.dc.application;

import io.undertow.server.HttpHandler;
import no.ssb.dc.api.http.Request;

import java.util.Set;

public interface Controller extends HttpHandler {

    String contextPath();

    Set<Request.Method> allowedMethods();

}
