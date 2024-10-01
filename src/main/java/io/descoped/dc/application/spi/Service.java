package io.descoped.dc.application.spi;

public interface Service {

    boolean isEnabled();

    void start();

    void stop();

}
