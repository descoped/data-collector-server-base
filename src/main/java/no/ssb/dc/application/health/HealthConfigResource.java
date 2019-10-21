package no.ssb.dc.application.health;

import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@HealthRenderPriority(priority = 5)
public class HealthConfigResource implements HealthResource {

    private final AtomicReference<Map<String, String>> configMap = new AtomicReference<>(new LinkedHashMap<>());

    @Override
    public String name() {
        return "configuration";
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return queryParams.containsKey("config");
    }

    @Override
    public Object resource() {
        return configMap.get();
    }

    public void setConfiguration(Map<String, String> configuration) {
        Map<String, String> map = configMap.get();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            if (entry.getKey().contains("pass")) {
                map.put(entry.getKey(), "************");
            } else {
                map.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
