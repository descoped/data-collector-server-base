package no.ssb.dc.application.controller;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import no.ssb.config.DynamicConfiguration;
import no.ssb.config.StoreBasedDynamicConfiguration;
import no.ssb.dc.application.server.UndertowApplication;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;

import static no.ssb.dc.api.util.CommonUtils.captureStackTrace;
import static no.ssb.dc.application.controller.CORSHandlerTest.findFree;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetricsControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsControllerTest.class);

    private static OkHttpClient client = new OkHttpClient.Builder().build();
    private static UndertowApplication server;

    @BeforeAll
    public static void beforeAll() {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .values("prometheus.defaultExports.disabled", "true")
                .build();
        server = UndertowApplication.initializeUndertowApplication(configuration, findFree(), null);
        server.start();
    }

    @AfterAll
    public static void afterAll() {
        server.stop();
    }

    static public HttpResponse GET(String uri, String... headers) {
        try {
            URL url = new URL("http", server.getHost(), server.getPort(), uri);
            Request.Builder requestBuilder = new Request.Builder().url(url);
            if (headers != null && headers.length > 1) {
                requestBuilder.headers(Headers.of(headers));
            }

            Request request = requestBuilder.get().build();
            try (Response response = client.newCall(request).execute()) {
                return new HttpResponse(response.code(), response.body().string());

            }
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    @Test
    public void thatMetricsAreHealth() {
        HttpResponse response = GET("/metrics/-/healthy");
        assertEquals(200, response.statusCode);
        assertEquals("Exporter is Healthy.", response.body);
    }

    @Test
    public void thatMetricsAreReported() throws IOException, InterruptedException {
        HttpResponse response = GET("/metrics");
        assertEquals(200, response.statusCode);
        LOG.trace("body:\n{}", response.body);
    }

    @Test
    public void experiment() throws InterruptedException {
        Counter requests = Counter.build("requests_total", "Total requests").namespace("ns").subsystem("foo").register();
        requests.inc();

        Counter request_count = Counter.build("requests_count", "Count requests").namespace("ns").subsystem("foo").labelNames("url").register();
        requests.inc();

        Gauge gauge = Gauge.build("currentTime", "Current Time").namespace("ns").subsystem("foo").register();
        gauge.set(Instant.now().toEpochMilli());

        {
            // null value causes NPE
            Optional<String> emptyString = Optional.empty();
            request_count.labels(emptyString.orElse("")).inc();
        }

        {
            Histogram histogram = Histogram.build("histogram_KEY", "Histogram").register();
            {
                Histogram.Timer timer = histogram.startTimer();
                Thread.sleep(250);
                histogram.observe(1500);
                timer.observeDuration();
            }
            Thread.sleep(500);

            {
                Histogram.Timer timer = histogram.startTimer();
                Thread.sleep(220);
                histogram.observe(1200);
                timer.observeDuration();
            }
        }

        {
            Summary summary = Summary.build("someSummary", "Some Summary").register();
            Summary.Timer timer = summary.startTimer();
            Thread.sleep(250);
            summary.observe(1500);
            timer.observeDuration();
        }


    }

    static class HttpResponse {
        public final int statusCode;
        public final String body;

        public HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
