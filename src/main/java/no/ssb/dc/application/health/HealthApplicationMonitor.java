package no.ssb.dc.application.health;

import no.ssb.dc.api.health.HealthResourceUtils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class HealthApplicationMonitor {

    private final AtomicReference<ServerStatus> serverStatus = new AtomicReference<>(ServerStatus.SHUTDOWN);
    private final AtomicReference<String> host = new AtomicReference<>();
    private final AtomicInteger port = new AtomicInteger();
    private final AtomicLong since = new AtomicLong(0);

    public ServerStatus getServerStatus() {
        return serverStatus.get();
    }

    public void setServerStatus(ServerStatus status) {
        serverStatus.set(status);
        if (ServerStatus.RUNNING == status) {
            since.set(Instant.now().toEpochMilli());
        }
    }

    public String getHost() {
        return host.get();
    }

    public void setHost(String host) {
        this.host.set(host);
    }

    public Integer getPort() {
        return port.get();
    }

    public void setPort(Integer port) {
        this.port.set(port);
    }

    public String getSince() {
        return Instant.ofEpochMilli(since.get()).toString();
    }

    public String getNow() {
        return Instant.ofEpochMilli(Instant.now().toEpochMilli()).toString();
    }

    public String getUptime() {
        return HealthResourceUtils.durationAsString(since.get());
    }

    public enum ServerStatus {
        INITIALIZING,
        INITIALIZED,
        RUNNING,
        SHUTTING_DOWN,
        SHUTDOWN
    }

}
