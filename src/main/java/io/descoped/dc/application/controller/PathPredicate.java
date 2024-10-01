package io.descoped.dc.application.controller;

import io.descoped.dc.api.http.Request;

import java.util.Objects;

class PathPredicate implements Comparable<PathPredicate> {

    final Integer index;
    final String pathElement;
    final Request.Method method;

    private PathPredicate(Integer index, String pathElement, Request.Method method) {
        this.index = index;
        this.pathElement = pathElement;
        this.method = method;
    }

    static PathPredicate of(Integer index, String pathElement, Request.Method method) {
        return new PathPredicate(index, pathElement, method);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathPredicate that = (PathPredicate) o;
        return Objects.equals(index, that.index) &&
                Objects.equals(pathElement, that.pathElement) &&
                method == that.method;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, pathElement, method);
    }

    @Override
    public int compareTo(PathPredicate that) {
        int indexCompare = this.index.compareTo(that.index);
        //int pathCompare = this.pathElement.compareTo(that.pathElement);
        int methodCompare = this.method.compareTo(that.method);
        return Integer.compare(indexCompare, methodCompare);
    }

    @Override
    public String toString() {
        return "PathPredicate{" +
                "index=" + index +
                ", pathElement='" + pathElement + '\'' +
                ", method=" + method +
                '}';
    }
}
