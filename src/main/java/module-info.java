module no.ssb.dc.application {

    requires no.ssb.service.provider.api;
    requires no.ssb.config;
    requires no.ssb.dc.api;
    requires no.ssb.rawdata.api;

    requires org.slf4j;
    requires io.github.classgraph;

    requires undertow.core;

    exports no.ssb.dc.application;
    exports no.ssb.dc.application.service;
    exports no.ssb.dc.application.controller;
}
