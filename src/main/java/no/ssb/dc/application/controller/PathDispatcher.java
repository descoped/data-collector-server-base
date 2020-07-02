package no.ssb.dc.application.controller;

import io.undertow.server.HttpServerExchange;
import no.ssb.dc.api.http.HttpStatus;
import no.ssb.dc.api.http.Request;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PathDispatcher {

    private final Map<PathPredicate, PathAction> dispatchHandlers = new LinkedHashMap<>();

    private PathDispatcher() {
    }

    public static PathDispatcher create() {
        return new PathDispatcher();
    }

    boolean isVariableExpression(String element) {
        return element.startsWith("{") && element.endsWith("}");
    }

    /**
     * rules
     * <p>
     * /a equals /a/{b} = true
     * /a/b equals /a/{b} = true
     * /a/b equals /a/{c} = true
     *
     * @param templatePath
     * @param method
     * @param handlerCallback
     */
    public void bind(String templatePath, Request.Method method, Function<PathHandler, HttpStatus> handlerCallback) {
        PathParser templatePathParser = PathParser.create(templatePath);
        List<String> templatePathElements = templatePathParser.elements();
        int lastTemplatePathElementIndex = templatePathElements.size() - 1;
        String lastTemplatePathElement = templatePathElements.get(lastTemplatePathElementIndex);

        // ceiling key
        PathPredicate lastTemplatePathElementPredicate = PathPredicate.of(lastTemplatePathElementIndex, lastTemplatePathElement, method);
        Map.Entry<PathPredicate, PathAction> lastTemplatePathElementCeilingEntry = ceilingEntry(lastTemplatePathElementPredicate);

        // validate ceiling key with predicate
        if (lastTemplatePathElementCeilingEntry != null) {
            PathPredicate lastTemplatePathElementCeilingKey = lastTemplatePathElementCeilingEntry.getKey();
            if (lastTemplatePathElementIndex != lastTemplatePathElementCeilingKey.index || !method.equals(lastTemplatePathElementCeilingKey.method)) {
                lastTemplatePathElementCeilingEntry = null;
            }
        }

        // create actions for last template path element
        for (int i = lastTemplatePathElementIndex; i >= 0; i--) {
            // create binding for last template path element
            PathPredicate pathPredicate = PathPredicate.of(i, templatePathElements.get(i), method);
            if (i == lastTemplatePathElementIndex && lastTemplatePathElementCeilingEntry == null) {
                dispatchHandlers.computeIfAbsent(pathPredicate, key -> PathAction.of(templatePathParser, handlerCallback));

                // re-create binding for empty action handler
            } else if (i == lastTemplatePathElementIndex && lastTemplatePathElementCeilingEntry.getValue().isEmpty()) {
                dispatchHandlers.computeIfPresent(pathPredicate, (key, action) -> PathAction.of(templatePathParser, handlerCallback));

                // create void handlers lower in path than last template path element index
            } else {
                dispatchHandlers.computeIfAbsent(pathPredicate, action -> PathAction.empty());
            }
        }
    }

    Map<PathPredicate, PathAction> dispatchHandlers() {
        Map<PathPredicate, PathAction> sortedMap = new LinkedHashMap<>();
        dispatchHandlers.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> sortedMap.put(entry.getKey(), entry.getValue()));
        return sortedMap;
    }

    Map.Entry<PathPredicate, PathAction> ceilingEntry(PathPredicate pathPredicate) {
        Map<PathPredicate, PathAction> sortedMap = dispatchHandlers();
        Map.Entry<PathPredicate, PathAction> ceiling = null;
        for (Map.Entry<PathPredicate, PathAction> entry : sortedMap.entrySet()) {
            // if index, method and path length is satisfied, it is a ceilingEntry
            if (ceiling == null && pathPredicate.index.equals(entry.getKey().index) && pathPredicate.pathElement.equals(entry.getKey().pathElement)) {
                ceiling = entry;
            }

            if (ceiling == null && pathPredicate.index.equals(entry.getKey().index) && isVariableExpression(entry.getKey().pathElement)) {
                ceiling = entry;
            }

            if (ceiling != null && pathPredicate.index.equals(entry.getKey().index) && pathPredicate.method.equals(entry.getKey().method)) {
                ceiling = entry;
                break;
            }
        }
        return ceiling;
    }

    public PathHandler dispatch(String requestPath, Request.Method method, HttpServerExchange exchange) {
        PathParser requestPathParser = PathParser.create(requestPath);
        List<String> requestPathElements = requestPathParser.elements();
        int lastRequestPathElementIndex = requestPathElements.size() - 1;
        String lastRequestPathElement = requestPathElements.get(lastRequestPathElementIndex);

        Map.Entry<PathPredicate, PathAction> actionEntry = ceilingEntry(PathPredicate.of(lastRequestPathElementIndex, lastRequestPathElement, method));
        if (actionEntry == null) {
            throw new RuntimeException("Unable to resolve action-handler for: " + requestPath);
        }

        PathPredicate templatePathPredicate = actionEntry.getKey();
        if (!method.equals(templatePathPredicate.method)) {
            throw new RuntimeException("Found non-matching verb action-handler for: " + method + " " + requestPath + " -> Found: " + actionEntry);
        }

        PathParser templateParser = actionEntry.getValue().templateParser;
        if (templateParser == null || lastRequestPathElementIndex != templateParser.elements().size() - 1) {
            throw new RuntimeException("Panic! The action-handler for: " + requestPath + " is incorrect! Found: " + actionEntry);
        }

        PathBindings bindings = templateParser.path(requestPath);
        if (!bindings.isSatisfied()) {
            throw new RuntimeException(String.format("RequestPath: %s DID NOT satisfy templatePath: %s", requestPath, bindings.template().path()));
        }
        Map<String, String> variables = bindings.extractVariables();

        Function<PathHandler, HttpStatus> callback = actionEntry.getValue().pathHandler;
        PathHandler pathHandler = new PathHandler(exchange, variables);

        try {
            HttpStatus status = callback.apply(pathHandler);
            pathHandler.statusCode(status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return pathHandler;
    }
}
