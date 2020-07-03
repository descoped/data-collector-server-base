package no.ssb.dc.application.controller;

import java.util.List;
import java.util.Objects;

public class PathParser {

    private final UriTemplate template;

    private PathParser(String template) {
        this.template = new UriTemplate(template);
    }

    public static PathParser create(String template) {
        return new PathParser(template);
    }

    public List<String> elements() {
        return template.elements();
    }

    public PathBindings path(String url) {
        UriTemplate elements = new UriTemplate(url);
        return new PathBindings(template, elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathParser that = (PathParser) o;
        return Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(template);
    }
}
