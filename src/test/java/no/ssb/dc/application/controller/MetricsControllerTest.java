package no.ssb.dc.application.controller;

import io.prometheus.client.Counter;
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

import static no.ssb.dc.api.util.CommonUtils.captureStackTrace;
import static no.ssb.dc.application.controller.CORSHandlerTest.findFree;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetricsControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsControllerTest.class);

    private static OkHttpClient client = new OkHttpClient.Builder().build();
    private static UndertowApplication server;

    @BeforeAll
    static void beforeAll() {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder().build();
        server = UndertowApplication.initializeUndertowApplication(configuration, findFree());
        server.start();
    }

    @AfterAll
    static void afterAll() {
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

    static class HttpResponse {
        public final int statusCode;
        public final String body;

        public HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    @Test
    void thatMetricsAreHealth() {
        HttpResponse response = GET("/metrics/-/healthy");
        assertEquals(200, response.statusCode);
        assertEquals("Exporter is Healthy.", response.body);
    }

    @Test
    void thatMetricsAreReported() throws IOException {
        final Counter requests = Counter.build().name("requests_total").help("Total requests.").register();
        requests.inc();
        requests.inc();

        HttpResponse response = GET("/metrics");
        assertEquals(200, response.statusCode);
        LOG.trace("body:\n{}", response.body);
    }

}
