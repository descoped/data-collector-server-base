package no.ssb.dc.application.ssl;

import no.ssb.dc.api.security.BusinessSSLResource;

import java.util.function.Supplier;

public class BusinessSSLResourceSupplier {

    private final Supplier<? extends BusinessSSLResource> sslResourceSupplier;

    public BusinessSSLResourceSupplier(Supplier<? extends BusinessSSLResource> sslResourceSupplier) {
        this.sslResourceSupplier = sslResourceSupplier;
    }

    public Supplier<? extends BusinessSSLResource> get() {
        return sslResourceSupplier;
    }
}
