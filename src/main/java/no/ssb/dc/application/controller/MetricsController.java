package no.ssb.dc.application.controller;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.api.http.Request;
import no.ssb.dc.application.health.HealthResourceFactory;
import no.ssb.dc.application.spi.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetricsController implements Controller {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsController.class);
    private final static String HEALTHY_RESPONSE = "Exporter is Healthy.";
    private final HealthResourceFactory healthResourceFactory;
    private final CollectorRegistry collectorRegistry;

    public MetricsController(HealthResourceFactory healthResourceFactory) {
        this.healthResourceFactory = healthResourceFactory;
        collectorRegistry = CollectorRegistry.defaultRegistry;
    }

    @Override
    public String contextPath() {
        return "/metrics";
    }

    @Override
    public Set<Request.Method> allowedMethods() {
        return Set.of(Request.Method.GET);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        if ("get".equalsIgnoreCase(exchange.getRequestMethod().toString())) {
            if (exchange.getRequestPath().startsWith("/metrics")) {
                dealWithHealthInfo(exchange);
                return;
            }
        }

        exchange.setStatusCode(400);
    }

    private void dealWithHealthInfo(HttpServerExchange exchange) {
        List<HealthResource> healthResources = healthResourceFactory.getHealthResources();
        for (HealthResource healthResource : healthResources) {
            LOG.trace("{}", healthResource.toString());
        }

        StringWriter sw = new StringWriter();

        if (exchange.getRequestPath().endsWith("/-/healthy")) {
            sw.write(HEALTHY_RESPONSE);
        } else {
            try {
                String query = exchange.getQueryString();
                TextFormat.write004(sw, collectorRegistry.filteredMetricFamilySamples(parseQuery(query)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        exchange.setStatusCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(sw.toString());
    }

    protected static Set<String> parseQuery(String query) throws UnsupportedEncodingException {
        Set<String> names = new HashSet<String>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx != -1 && URLDecoder.decode(pair.substring(0, idx), "UTF-8").equals("name[]")) {
                    names.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        }
        return names;
    }
}
