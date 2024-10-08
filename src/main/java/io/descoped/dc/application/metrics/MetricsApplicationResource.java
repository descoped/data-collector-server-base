package io.descoped.dc.application.metrics;

import io.descoped.dc.api.metrics.MetricsResource;
import io.prometheus.client.Gauge;

import java.time.Instant;

public class MetricsApplicationResource implements MetricsResource {

    private static final Gauge since = Gauge.build().namespace("dc").subsystem("application").name("since").help("Application started").register();
    private static final Gauge uptime = Gauge.build().namespace("dc").subsystem("application").name("uptime").help("Application uptime").register();

    public MetricsApplicationResource() {
        since.set(Instant.now().toEpochMilli());
    }

    public void trackUptime() {
        uptime.set(Instant.now().toEpochMilli() - since.get());
    }

}
