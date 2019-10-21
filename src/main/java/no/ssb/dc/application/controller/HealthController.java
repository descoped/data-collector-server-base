package no.ssb.dc.application.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.api.http.Request;
import no.ssb.dc.api.util.JsonParser;
import no.ssb.dc.application.Controller;

import java.util.List;
import java.util.Set;

public class HealthController implements Controller {

    @Override
    public String contextPath() {
        return "/health";
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
            JsonParser jsonParser = JsonParser.createJsonParser();
            ObjectNode rootNode = jsonParser.createObjectNode();

            List<HealthResource> healthResources = HealthResourceFactory.getInstance().getHealthResources();
            for (HealthResource healthResource : healthResources) {
                if (!healthResource.canRender(exchange.getQueryParameters())) {
                    continue;
                }

                if (healthResource.isList()) {
                    ArrayNode arrayNode = jsonParser.createArrayNode();
                    List<?> list = (List<?>) healthResource.resource();
                    for (Object item : list) {
                        ObjectNode convertedNode = jsonParser.mapper().convertValue(item, ObjectNode.class);
                        arrayNode.add(convertedNode);
                    }
                    rootNode.set(healthResource.name(), arrayNode);

                } else {
                    ObjectNode convertedNode = jsonParser.mapper().convertValue(healthResource.resource(), ObjectNode.class);
                    rootNode.set(healthResource.name(), convertedNode);
                }
            }

            String payload = jsonParser.toPrettyJSON(rootNode);

            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(payload);

            return;
        }

        exchange.setStatusCode(400);
    }
}
