package no.ssb.dc.application.health;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class HealthApplicationMonitor {

    private final AtomicReference<ServerStatus> serverStatus = new AtomicReference<>(ServerStatus.SHUTDOWN);
    private final AtomicReference<String> host = new AtomicReference<>();
    private final AtomicInteger port = new AtomicInteger();
    private final AtomicLong since = new AtomicLong(0);

    public static HealthApplicationMonitor instance() {
        return HealthApplicationStatusMonitorSingleton.INSTANCE;
    }

    public ServerStatus getServerStatus() {
        return serverStatus.get();
    }

    public void setServerStatus(ServerStatus status) {
        serverStatus.set(status);
        if (ServerStatus.RUNNING == status) {
            since.set(System.currentTimeMillis());
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

    public String getUptime() {
        long uptimeInMillis = since.get() - System.currentTimeMillis();
        return Instant.now().minus(uptimeInMillis, ChronoUnit.MILLIS).toString();
    }

    public enum ServerStatus {
        INITIALIZING,
        INITIALIZED,
        RUNNING,
        SHUTTING_DOWN,
        SHUTDOWN
    }

    private static class HealthApplicationStatusMonitorSingleton {
        private static final HealthApplicationMonitor INSTANCE = new HealthApplicationMonitor();
    }
}
