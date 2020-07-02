package no.ssb.dc.application.controller;

import java.util.function.Consumer;

class PathAction {

    final PathParser templateParser;
    final Consumer<PathHandler> pathHandler;

    private PathAction(PathParser templateParser, Consumer<PathHandler> pathHandler) {
        this.templateParser = templateParser;
        this.pathHandler = pathHandler;
    }

    static PathAction of(PathParser templateParser, Consumer<PathHandler> pathHandler) {
        return new PathAction(templateParser, pathHandler);
    }

    static PathAction empty() {
        return new PathAction(null, null);
    }

    boolean isEmpty() {
        return templateParser == null && pathHandler == null;
    }

}
