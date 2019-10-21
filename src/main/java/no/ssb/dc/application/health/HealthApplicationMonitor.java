package no.ssb.dc.application.health;

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
        long elapsedTime = System.currentTimeMillis() - since.get();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = elapsedTime / daysInMilli;
        elapsedTime = elapsedTime % daysInMilli;

        long elapsedHours = elapsedTime / hoursInMilli;
        elapsedTime = elapsedTime % hoursInMilli;

        long elapsedMinutes = elapsedTime / minutesInMilli;
        elapsedTime = elapsedTime % minutesInMilli;

        long elapsedSeconds = elapsedTime / secondsInMilli;

        return String.format(
                "%d days, %d hours, %d minutes, %d seconds",
                elapsedDays,
                elapsedHours,
                elapsedMinutes,
                elapsedSeconds
        );
    }

    public enum ServerStatus {
        INITIALIZING,
        INITIALIZED,
        RUNNING,
        SHUTTING_DOWN,
        SHUTDOWN
    }

}
