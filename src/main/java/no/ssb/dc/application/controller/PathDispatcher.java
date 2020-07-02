package no.ssb.dc.application.controller;

import io.undertow.server.HttpServerExchange;
import no.ssb.dc.api.http.Request;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

public class PathDispatcher {

    final NavigableMap<PathPredicate, PathAction> dispatchHandlers = new TreeMap<>();

    private int comparator(PathPredicate k1, PathPredicate k2) {
        return Integer.compare(k1.index.compareTo(k2.index), k1.method.compareTo(k2.method));
    }

    private PathDispatcher() {
    }

    public static PathDispatcher create() {
        return new PathDispatcher();
    }

    boolean isVariableExpression(String element) {
        return element.startsWith("{") && element.endsWith("}");
    }

    /**
     * compare new templatePath with requestPath
     * <p>
     * /a equals /a/{b} = true
     * /a/{b} equals /a/{b} = true
     * /a/{b} equals /a/{c} = true
     * /a/{b} equals /a = true
     *
     * @param templatePath
     * @param method
     * @param handlerCallback
     */
    public void bind(String templatePath, Request.Method method, Consumer<PathHandler> handlerCallback) {
        PathParser templatePathParser = PathParser.create(templatePath);
        List<String> templatePathElements = templatePathParser.elements();
        int lastTemplatePathElementIndex = templatePathElements.size() - 1;
        String lastTemplatePathElement = templatePathElements.get(lastTemplatePathElementIndex);

        // ceiling key
        PathPredicate lastTemplatePathElementPredicate = PathPredicate.of(lastTemplatePathElementIndex, lastTemplatePathElement, method);
        Map.Entry<PathPredicate, PathAction> lastTemplatePathElementCeilingEntry = dispatchHandlers.ceilingEntry(lastTemplatePathElementPredicate);

        // validate ceiling key with predicate
        if (lastTemplatePathElementCeilingEntry != null) {
            PathPredicate lastTemplatePathElementCeilingKey = lastTemplatePathElementCeilingEntry.getKey();
            if (lastTemplatePathElementIndex != lastTemplatePathElementCeilingKey.index || !method.equals(lastTemplatePathElementCeilingKey.method)) {
                lastTemplatePathElementCeilingEntry = null;
            }
        }

        /*
            todo if a template path element is variable and the name differs from an existing one, raise an error -- should we care
         */

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

    public PathHandler dispatch(String requestPath, Request.Method method, HttpServerExchange exchange) {
        PathParser requestPathParser = PathParser.create(requestPath);
        List<String> requestPathElements = requestPathParser.elements();
        int lastRequestPathElementIndex = requestPathElements.size() - 1;
        String lastTemplatePathElement = requestPathElements.get(lastRequestPathElementIndex);

        Map.Entry<PathPredicate, PathAction> actionEntry = dispatchHandlers.ceilingEntry(PathPredicate.of(lastRequestPathElementIndex, lastTemplatePathElement, method));
        if (actionEntry == null) {
            throw new RuntimeException("Unable to resolve action-handler for: " + requestPath);
        }

        PathPredicate templatePathPredicate = actionEntry.getKey();
        if (!method.equals(templatePathPredicate.method)) {
            throw new RuntimeException("Found non-matching verb action-handler for: " + method + " " + requestPath + " -> Found: " + actionEntry);
        }

        PathParser templateParser = actionEntry.getValue().templateParser;
        if (lastRequestPathElementIndex != templateParser.elements().size() - 1) {
            throw new RuntimeException("Panic! The action-handler for: " + requestPath + " is incorrect! Found: " + actionEntry);
        }

        PathBindings bindings = templateParser.path(requestPath);
        if (!bindings.isSatisfied()) {
            throw new RuntimeException(String.format("RequestPath: %s DID NOT satisfy templatePath: %s", requestPath, bindings.template().path()));
        }
        Map<String, String> variables = bindings.extractVariables();

        Consumer<PathHandler> callback = actionEntry.getValue().pathHandler;
        PathHandler pathHandler = new PathHandler(exchange, variables);
        callback.accept(pathHandler);

        return pathHandler;
    }
}
