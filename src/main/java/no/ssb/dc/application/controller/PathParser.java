package no.ssb.dc.application.controller;

import java.util.List;

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

}
