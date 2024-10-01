package io.descoped.dc.application.controller;

import io.descoped.dc.api.http.HttpStatus;

import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathAction that = (PathAction) o;
        return Objects.equals(templateParser, that.templateParser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateParser);
    }
}
