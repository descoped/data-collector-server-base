package no.ssb.dc.application.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;

import java.util.Deque;
import java.util.Map;

@HealthRenderPriority(priority = 1)
public class HealthApplicationResource implements HealthResource {

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
        HealthApplicationMonitor applicationMonitor = HealthApplicationMonitor.instance();
        return new ServerInfo(
                applicationMonitor.getServerStatus().name(),
                applicationMonitor.getHost(),
                applicationMonitor.getPort(),
                applicationMonitor.getUptime()
        );
    }

    @SuppressWarnings("WeakerAccess")
    public static class ServerInfo {
        @JsonProperty public final String status;
        @JsonProperty public final String host;
        @JsonProperty public final Integer port;
        @JsonProperty("since") public final String uptime;

        public ServerInfo(String status, String host, Integer port, String uptime) {
            this.status = status;
            this.host = host;
            this.port = port;
            this.uptime = uptime;
        }
    }
}
