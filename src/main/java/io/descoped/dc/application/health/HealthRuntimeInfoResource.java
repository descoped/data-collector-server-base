package io.descoped.dc.application.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@HealthRenderPriority(priority = 45)
public class HealthRuntimeInfoResource implements HealthResource {

    @Override
    public Optional<Boolean> isUp() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "runtime";
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return Set.of("runtime", "all").stream().anyMatch(queryParams::containsKey);
    }

    @Override
    public Object resource() {
        return new RuntimeInfo();
    }

    static class RuntimeInfo {
        @JsonProperty
        final int availableProcessors;
        @JsonProperty
        final long freeMemoryInMegaBytes;
        @JsonProperty
        final long maxMemoryInMegaBytes;
        @JsonProperty
        final long totalMemoryInMegaBytes;

        public RuntimeInfo() {
            Runtime runtime = Runtime.getRuntime();
            availableProcessors = runtime.availableProcessors();
            freeMemoryInMegaBytes = runtime.freeMemory() / (1024 * 1024);
            maxMemoryInMegaBytes = runtime.maxMemory() / (1024 * 1024);
            totalMemoryInMegaBytes = runtime.totalMemory() / (1024 * 1024);
        }
    }
}
