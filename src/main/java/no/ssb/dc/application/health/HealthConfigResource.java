package no.ssb.dc.application.health;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.api.util.JsonParser;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@HealthRenderPriority(priority = 30)
public class HealthConfigResource implements HealthResource {

    private final AtomicReference<ObjectNode> configNode = new AtomicReference<>(JsonParser.createJsonParser().createObjectNode());

    @Override
    public Optional<Boolean> isUp() {
        return Optional.empty();
    }

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
        return Set.of("config", "all").stream().anyMatch(queryParams::containsKey);
    }

    @Override
    public Object resource() {
        return configNode.get();
    }

    public void setConfiguration(Map<String, String> configuration) {
        Set<String> maskValues = Set.of("pass", "pwd", "secret", "credential", "token");
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            if (maskValues.stream().anyMatch(key -> entry.getKey().toLowerCase().contains(key))) {
                map.put(entry.getKey(), "************");
            } else {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        JsonParser jsonParser = JsonParser.createJsonParser();
        configNode.set(jsonParser.mapper().convertValue(map, ObjectNode.class));
    }
}
