package no.ssb.dc.application.controller;

import no.ssb.dc.api.http.HttpStatus;

import java.util.function.Function;

class PathAction {

    final PathParser templateParser;
    final Function<PathHandler, HttpStatus> pathHandler;

    private PathAction(PathParser templateParser, Function<PathHandler, HttpStatus> pathHandler) {
        this.templateParser = templateParser;
        this.pathHandler = pathHandler;
    }

    static PathAction of(PathParser templateParser, Function<PathHandler, HttpStatus> pathHandler) {
        return new PathAction(templateParser, pathHandler);
    }

    static PathAction empty() {
        return new PathAction(null, null);
    }

    boolean isEmpty() {
        return templateParser == null && pathHandler == null;
    }

}
