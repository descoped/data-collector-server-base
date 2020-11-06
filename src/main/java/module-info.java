module no.ssb.dc.application {

    requires no.ssb.service.provider.api;
    requires no.ssb.config;
    requires no.ssb.dc.api;

    requires org.slf4j;
    requires io.github.classgraph;
    requires com.fasterxml.jackson.databind;

    requires undertow.core;
    requires simpleclient.common;
    requires simpleclient;
    requires simpleclient.hotspot;

    requires freemarker;

    opens no.ssb.dc.application.health to com.fasterxml.jackson.databind;
    opens no.ssb.dc.application.engine; // open to unnamed module freemarker
    opens no.ssb.dc.application.metrics;

    exports no.ssb.dc.application.spi;
    exports no.ssb.dc.application.ssl;
    exports no.ssb.dc.application.server;
    exports no.ssb.dc.application.controller;
    exports no.ssb.dc.application.engine;
    exports no.ssb.dc.application.health;
    exports no.ssb.dc.application.metrics;
}
