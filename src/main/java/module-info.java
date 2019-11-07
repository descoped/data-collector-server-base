module no.ssb.dc.application {

    requires no.ssb.service.provider.api;
    requires no.ssb.config;
    requires no.ssb.dc.api;

    requires org.slf4j;
    requires io.github.classgraph;
    requires com.fasterxml.jackson.databind;

    requires undertow.core;

    requires freemarker;

    opens no.ssb.dc.application.health to com.fasterxml.jackson.databind;

    exports no.ssb.dc.application;
    exports no.ssb.dc.application.controller;
    exports no.ssb.dc.application.health;
}
