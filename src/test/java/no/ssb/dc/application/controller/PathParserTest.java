package no.ssb.dc.application.controller;

import io.undertow.server.HttpServerExchange;
import no.ssb.dc.api.http.Request;
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
    void parsePath() {
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
    void thatPathBindingsSatisfiesRequest() {
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
    void pathDispatcher() {
        PathDispatcher pathDispatcher = PathDispatcher.create();

        pathDispatcher.bind("/check-integrity", Request.Method.GET, this::handleCheckIntegrity);
        pathDispatcher.bind("/check-integrity/{topic}/full", Request.Method.GET, this::handleFullSummary);
        pathDispatcher.bind("/check-integrity/{topic}", Request.Method.GET, this::handleCheckIntegrityTopic);
        pathDispatcher.bind("/check-integrity/{topic}", Request.Method.DELETE, this::handleDeleteCheckIntegrityTopic);
        pathDispatcher.dispatchHandlers.forEach((key,valeu) -> LOG.trace("{}", key.toString()));

        HttpServerExchange mockExchange = new HttpServerExchange(null);
        assertEquals(true, pathDispatcher.dispatch("/check-integrity", Request.Method.GET, mockExchange).outcome().get("INVOKED"));
        assertEquals(true, pathDispatcher.dispatch("/check-integrity/some-topic", Request.Method.GET, mockExchange).outcome().get("INVOKED"));
        assertEquals(true, pathDispatcher.dispatch("/check-integrity/some-topic", Request.Method.DELETE, mockExchange).outcome().get("INVOKED"));
        assertEquals(true, pathDispatcher.dispatch("/check-integrity/another-topic/full", Request.Method.GET, mockExchange).outcome().get("INVOKED"));
    }

    @Test
    void name() {
        int stringCompare = "foo".compareTo("foo");
        int intCompare = Integer.valueOf(10).compareTo(Integer.valueOf(10));
        int enumCompare = Request.Method.GET.compareTo(Request.Method.DELETE);
        int enum2Compare = Request.Method.GET.compareTo(Request.Method.GET);
        int compare = Integer.compare(intCompare, enumCompare);
        int compare2 = Integer.compare(intCompare, enum2Compare);
        LOG.trace("{} -- {} -- {} -- {} -- {}", intCompare, stringCompare, enumCompare, compare, compare2);

        PathPredicate p1 = PathPredicate.of(1, "item", Request.Method.GET);
        PathPredicate p2 = PathPredicate.of(1, "item", Request.Method.DELETE);
        LOG.trace("{} -- {} -- {} -- {}", p1.compareTo(p2), p1.equals(p2), p1.hashCode(), p2.hashCode());
    }

    private void handleCheckIntegrity(PathHandler pathHandler) {
        LOG.trace("handleCheckIntegrity invoked: {}", pathHandler.parameters());
        pathHandler.outcome().put("INVOKED", true);
    }

    private void handleCheckIntegrityTopic(PathHandler pathHandler) {
        LOG.trace("handleCheckIntegrityTopic invoked: {}", pathHandler.parameters());
        pathHandler.outcome().put("INVOKED", true);
    }

    private void handleDeleteCheckIntegrityTopic(PathHandler pathHandler) {
        LOG.trace("handleDeleteCheckIntegrityTopic invoked: {}", pathHandler.parameters());
        pathHandler.outcome().put("INVOKED", true);
    }

    private void handleFullSummary(PathHandler pathHandler) {
        LOG.trace("handleFullSummary invoked: {}", pathHandler.parameters());
        pathHandler.outcome().put("INVOKED", true);
    }

}
