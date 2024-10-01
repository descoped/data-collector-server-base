package io.descoped.dc.application.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.application.controller.CORSHandler;
import io.descoped.dc.application.controller.DispatchController;
import io.descoped.dc.application.controller.PingController;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@HealthRenderPriority(priority = 40)
public class HealthContextsResource implements HealthResource {

    private final List<ControllerContext> contexts = new ArrayList<>();

    public HealthContextsResource() {
        add("", Set.of(Request.Method.OPTIONS), CORSHandler.class);
        add("", Set.of(Request.Method.values()), DispatchController.class);
        add("/ping", Set.of(Request.Method.GET), PingController.class);
    }

    @Override
    public Optional<Boolean> isUp() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "contexts";
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return Set.of("contexts", "all").stream().anyMatch(queryParams::containsKey);
    }

    @Override
    public Object resource() {
        return contexts;
    }

    public void add(String contextPath, Set<Request.Method> allowedMethods, Class<?> controller) {
        contexts.add(new ControllerContext(contextPath, allowedMethods, controller));
    }

    static class ControllerContext {
        @JsonProperty
        public final String contextPath;
        @JsonProperty
        public final Set<Request.Method> allowedMethods;
        @JsonProperty
        public final Class<?> controller;

        ControllerContext(String contextPath, Set<Request.Method> allowedMethods, Class<?> controller) {
            this.contextPath = contextPath;
            this.allowedMethods = allowedMethods;
            this.controller = controller;
        }
    }
}
