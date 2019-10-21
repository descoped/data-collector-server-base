package no.ssb.dc.application.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.api.http.Request;
import no.ssb.dc.application.controller.CORSController;
import no.ssb.dc.application.controller.DispatchController;
import no.ssb.dc.application.controller.PingController;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

@HealthRenderPriority(priority = 8)
public class HealthContextsResource implements HealthResource {

    private final List<ControllerContext> contexts = new ArrayList<>();

    public HealthContextsResource() {
        add("", Set.of(Request.Method.OPTIONS), CORSController.class);
        add("", Set.of(Request.Method.values()), DispatchController.class);
        add("/ping", Set.of(Request.Method.GET), PingController.class);
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
        return queryParams.containsKey("contexts");
    }

    @Override
    public Object resource() {
        return contexts;
    }

    public void add(String contextPath, Set<Request.Method> allowedMethods, Class<?> controller) {
        contexts.add(new ControllerContext(contextPath, allowedMethods, controller));
    }

    static class ControllerContext {
        @JsonProperty public final String contextPath;
        @JsonProperty public final Set<Request.Method> allowedMethods;
        @JsonProperty public final Class<?> controller;

        ControllerContext(String contextPath, Set<Request.Method> allowedMethods, Class<?> controller) {
            this.contextPath = contextPath;
            this.allowedMethods = allowedMethods;
            this.controller = controller;
        }
    }
}
