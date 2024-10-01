module io.descoped.dc.application {

    requires io.descoped.service.provider.api;
    requires io.descoped.dynamic.config;
    requires io.descoped.dc.api;
    requires secrets.client.api;

    requires org.slf4j;
    requires io.github.classgraph;
    requires com.fasterxml.jackson.databind;

    requires undertow.core;
    requires simpleclient.common;
    requires simpleclient;
    requires simpleclient.hotspot;

    requires freemarker;

    opens io.descoped.dc.application.health to com.fasterxml.jackson.databind;
    opens io.descoped.dc.application.engine; // open to unnamed module freemarker
    opens io.descoped.dc.application.metrics;

    exports io.descoped.dc.application.spi;
    exports io.descoped.dc.application.ssl;
    exports io.descoped.dc.application.server;
    exports io.descoped.dc.application.controller;
    exports io.descoped.dc.application.engine;
    exports io.descoped.dc.application.health;
    exports io.descoped.dc.application.metrics;
}
