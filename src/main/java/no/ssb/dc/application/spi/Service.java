package no.ssb.dc.application.spi;

public interface Service {

    boolean isEnabled();

    void start();

    void stop();

}
