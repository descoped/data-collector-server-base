package no.ssb.dc.application.ssl;

import no.ssb.dc.api.security.BusinessSSLBundle;

import java.util.function.Supplier;

public class BusinessSSLBundleSupplier implements Supplier<BusinessSSLBundle> {

    private final Supplier<BusinessSSLBundle> sslBundleSupplier;

    public BusinessSSLBundleSupplier(Supplier<BusinessSSLBundle> sslBundleSupplier) {
        this.sslBundleSupplier = sslBundleSupplier;
    }

    @Override
    public BusinessSSLBundle get() {
        return sslBundleSupplier != null ? sslBundleSupplier.get() : null;
    }
}
