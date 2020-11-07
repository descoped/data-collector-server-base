package no.ssb.dc.application.ssl;

import no.ssb.dc.api.security.ProvidedBusinessSSLResource;

import java.util.function.Supplier;

public class BusinessSSLResourceSupplier implements Supplier<ProvidedBusinessSSLResource> {

    private final Supplier<ProvidedBusinessSSLResource> sslResourceSupplier;

    public BusinessSSLResourceSupplier(Supplier<ProvidedBusinessSSLResource> sslResourceSupplier) {
        this.sslResourceSupplier = sslResourceSupplier;
    }

    @Override
    public ProvidedBusinessSSLResource get() {
        return sslResourceSupplier != null ? sslResourceSupplier.get() : null;
    }
}
