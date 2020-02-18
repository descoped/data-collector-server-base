package no.ssb.dc.application.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.api.http.HttpStatusCode;
import no.ssb.dc.api.http.Request;
import no.ssb.dc.api.util.JsonParser;
import no.ssb.dc.application.health.HealthResourceFactory;
import no.ssb.dc.application.server.Controller;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class HealthController implements Controller {

    private final HealthResourceFactory healthResourceFactory;

    public HealthController(HealthResourceFactory healthResourceFactory) {
        this.healthResourceFactory = healthResourceFactory;
    }

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
            if ("/health".equals(exchange.getRequestPath())) {
                dealWithHealthInfo(exchange);
                return;
            } else if ("/health/alive".equals(exchange.getRequestPath())) {
                checkIfServiceIsAlive(exchange);
                return;
            } else if ("/health/ready".equals(exchange.getRequestPath())) {
                checkIfServiceIsReady(exchange);
                return;
            }
        }

        exchange.setStatusCode(400);
    }

    private void dealWithHealthInfo(HttpServerExchange exchange) {
        JsonParser jsonParser = JsonParser.createJsonParser();
        ObjectNode rootNode = jsonParser.createObjectNode();

        List<HealthResource> healthResourcesUp = healthResourceFactory.getHealthResources();
        Predicate<HealthResource> resourceSupportsServiceUp = healthResource -> healthResource.isUp().orElse(true);
        rootNode.put("status", State.checkState(healthResourcesUp.stream().allMatch(resourceSupportsServiceUp)).name());

        List<HealthResource> healthResources = healthResourceFactory.getHealthResources();
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
    }


    /*
     * Requirements: https://github.com/statisticsnorway/ssb-developer-guide/blob/master/docs/platform.md#liveness-and-readiness-probes
     */

    private void checkIfServiceIsAlive(HttpServerExchange exchange) {
        exchange.setStatusCode(HttpStatusCode.HTTP_OK.statusCode());
    }

    private void checkIfServiceIsReady(HttpServerExchange exchange) {
        // todo consideration: add last history worker unless there are an active worker running
        List<HealthResource> healthResourcesUp = healthResourceFactory.getHealthResources();
        // only resources that should take part in service readiness must be advised
        Predicate<HealthResource> resourceSupportsServiceUp = healthResource -> healthResource.isUp().orElse(true);
        boolean isHealthy = healthResourcesUp.stream().allMatch(resourceSupportsServiceUp);
        int statusCode = isHealthy ? HttpStatusCode.HTTP_OK.statusCode() : HttpStatusCode.HTTP_UNAVAILABLE.statusCode();
        exchange.setStatusCode(statusCode);
    }

    enum State {
        UP,
        DOWN;

        static State checkState(boolean isUp) {
            return isUp ? UP : DOWN;
        }
    }
}
