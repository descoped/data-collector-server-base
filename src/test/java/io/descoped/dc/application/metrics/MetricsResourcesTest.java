package io.descoped.dc.application.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

public class MetricsResourcesTest {

    @Test
    void testMetricsFactory() throws InterruptedException, IOException {
        MetricsResourceFactory factory = MetricsResourceFactory.create();
        MetricsApplicationResource applicationResource = factory.get(MetricsApplicationResource.class);
        Thread.sleep(250);
        applicationResource.trackUptime();
        StringWriter sw = new StringWriter();
        TextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
        System.out.printf("%s%n", sw.toString());
    }
}
