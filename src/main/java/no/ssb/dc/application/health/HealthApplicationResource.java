package no.ssb.dc.application.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;

import java.util.Deque;
import java.util.Map;

@HealthRenderPriority(priority = 1)
public class HealthApplicationResource implements HealthResource {

    private final HealthApplicationMonitor monitor;

    public HealthApplicationResource() {
        monitor = new HealthApplicationMonitor();
    }

    @Override
    public String name() {
        return "application";
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return true;
    }

    @Override
    public Object resource() {
        return new ServerInfo(
                monitor.getServerStatus().name(),
                monitor.getHost(),
                monitor.getPort(),
                monitor.getSince(),
                monitor.getNow(),
                monitor.getUptime());
    }

    public HealthApplicationMonitor getMonitor() {
        return monitor;
    }

    @SuppressWarnings("WeakerAccess")
    public static class ServerInfo {
        @JsonProperty public final String status;
        @JsonProperty public final String host;
        @JsonProperty public final Integer port;
        @JsonProperty public final String since;
        @JsonProperty public final String now;
        @JsonProperty public final String uptime;

        public ServerInfo(String status, String host, Integer port, String since, String now, String uptime) {
            this.status = status;
            this.host = host;
            this.port = port;
            this.since = since;
            this.now = now;
            this.uptime = uptime;
        }
    }
}
