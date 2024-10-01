package io.descoped.dc.application.controller;

import io.descoped.dc.api.http.HttpStatus;
import io.descoped.dc.api.http.Request;
import io.undertow.server.HttpServerExchange;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathParserTest {

    private static final Logger LOG = LoggerFactory.getLogger(PathParserTest.class);

    @Test
    public void parsePath() {
        String template = "/a/{b}/c";

        PathParser pathParser = PathParser.create(template);

        {
            PathBindings pathBindings = pathParser.path("/a/1/c");
            assertEquals(3, pathBindings.request().size());
            Map<String, String> variables = pathBindings.extractVariables();
            assertTrue(variables.containsKey("b"), "Unresolved parameter b");
            assertEquals("1", variables.get("b"));
        }

        {
            String uuid = UUID.randomUUID().toString();
            PathBindings pathBindings = pathParser.path(String.format("/a/%s/c", uuid));
            assertEquals(3, pathBindings.request().size());
            Map<String, String> variables = pathBindings.extractVariables();
            assertTrue(variables.containsKey("b"), "Unresolved variable 'b'");
            assertEquals(uuid, variables.get("b"));
        }

        {
            PathBindings pathBindings = pathParser.path("/a/a-05_b#$/c");
            assertEquals(3, pathBindings.request().size());
            Map<String, String> variables = pathBindings.extractVariables();
            assertTrue(variables.containsKey("b"), "Unresolved variable 'b'");
            assertEquals("a-05_b#$", variables.get("b"));
        }
    }

    @Test
    public void thatPathBindingsSatisfiesRequest() {
        String template = "/check-integrity/{topic}/full";

        PathParser pathParser = PathParser.create(template);

        {
            PathBindings pathBindings = pathParser.path("/check-integrity");
            assertEquals(1, pathBindings.length());
            assertTrue(pathBindings.isSatisfied(), "Invalid path");
        }

        {
            PathBindings pathBindings = pathParser.path("/check-integrity/some-topic");
            assertEquals(2, pathBindings.length());
            assertTrue(pathBindings.isSatisfied(), "Invalid path");
            Map<String, String> variables = pathBindings.extractVariables();
            assertEquals("some-topic", variables.get("topic"));
        }

        {
            PathBindings pathBindings = pathParser.path("/check-integrity/some-topic/full");
            assertEquals(3, pathBindings.length());
            assertTrue(pathBindings.isSatisfied(), "Invalid path");
            Map<String, String> variables = pathBindings.extractVariables();
            assertEquals("some-topic", variables.get("topic"));
        }

        {
            PathBindings pathBindings = pathParser.path("/check-integrity/some-topic/less");
            assertEquals(3, pathBindings.length());
            assertFalse(pathBindings.isSatisfied(), "Invalid path");
            Map<String, String> variables = pathBindings.extractVariables();
            assertEquals("some-topic", variables.get("topic"));
        }
    }

    @Test
    public void pathDispatcher() {
        PathDispatcher pathDispatcher = PathDispatcher.create();

        pathDispatcher.bind("/check-integrity", Request.Method.GET, this::handleCheckIntegrity);
        pathDispatcher.bind("/check-integrity/{topic}/full", Request.Method.GET, this::handleFullSummary);
        pathDispatcher.bind("/check-integrity/{topic}", Request.Method.GET, this::handleCheckIntegrityTopic);
        pathDispatcher.bind("/check-integrity/{foo}", Request.Method.PUT, this::handleCheckIntegrityTopic);
        pathDispatcher.bind("/check-integrity/{topic}", Request.Method.DELETE, this::handleDeleteCheckIntegrityTopic);
        pathDispatcher.dispatchHandlers().forEach((key, value) -> LOG.trace("{}", key.toString()));

        HttpServerExchange mockExchange = new HttpServerExchange(null);
        assertEquals(true, pathDispatcher.dispatch("/check-integrity", Request.Method.GET, mockExchange).outcome().get("INVOKED"));
        assertEquals(true, pathDispatcher.dispatch("/check-integrity/some-topic", Request.Method.GET, mockExchange).outcome().get("INVOKED"));
        assertEquals(true, pathDispatcher.dispatch("/check-integrity/some-topic", Request.Method.DELETE, mockExchange).outcome().get("INVOKED"));
        assertEquals(true, pathDispatcher.dispatch("/check-integrity/put-topic", Request.Method.PUT, mockExchange).outcome().get("INVOKED"));
        assertEquals(true, pathDispatcher.dispatch("/check-integrity/another-topic/full", Request.Method.GET, mockExchange).outcome().get("INVOKED"));
    }

    private HttpStatus handleCheckIntegrity(PathHandler pathHandler) {
        LOG.trace("handleCheckIntegrity invoked: {}", pathHandler.parameters());
        pathHandler.outcome().put("INVOKED", true);
        return HttpStatus.HTTP_OK;
    }

    private HttpStatus handleCheckIntegrityTopic(PathHandler pathHandler) {
        LOG.trace("handleCheckIntegrityTopic invoked: {}", pathHandler.parameters());
        pathHandler.outcome().put("INVOKED", true);
        return HttpStatus.HTTP_OK;
    }

    private HttpStatus handleDeleteCheckIntegrityTopic(PathHandler pathHandler) {
        LOG.trace("handleDeleteCheckIntegrityTopic invoked: {}", pathHandler.parameters());
        pathHandler.outcome().put("INVOKED", true);
        return HttpStatus.HTTP_OK;
    }

    private HttpStatus handleFullSummary(PathHandler pathHandler) {
        LOG.trace("handleFullSummary invoked: {}", pathHandler.parameters());
        pathHandler.outcome().put("INVOKED", true);
        return HttpStatus.HTTP_OK;
    }

}
