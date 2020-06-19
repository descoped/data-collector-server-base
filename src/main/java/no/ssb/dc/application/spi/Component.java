package no.ssb.dc.application.spi;

public interface Component extends AutoCloseable {

    void initialize();

    boolean isOpen();

    <R> R getDelegate();

}
