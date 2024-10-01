package io.descoped.dc.application.ssl;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.security.BusinessSSLResource;
import io.descoped.secrets.api.SecretManagerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.descoped.secrets.api.SecretManagerClient.safeCharArrayAsUTF8;

public class SecretManagerSSLResource implements BusinessSSLResource {

    private static final Logger LOG = LoggerFactory.getLogger(SecretManagerSSLResource.class);

    private final DynamicConfiguration configuration;
    private final boolean hasBusinessSslResourceProvider;
    private final String businessSslResourceProvider;
    private final SecretManagerClient secretManagerClient;

    public SecretManagerSSLResource(DynamicConfiguration configuration) {
        this.configuration = configuration;
        this.businessSslResourceProvider = configuration.evaluateToString("data.collector.sslBundle.provider");
        if (businessSslResourceProvider == null) {
            throw new RuntimeException("SSL Bundle Provider is NOT defined! Please check your config!");
        }
        this.hasBusinessSslResourceProvider = "google-secret-manager".equals(businessSslResourceProvider);
        LOG.info("Create BusinessSSL resource provider: {}", businessSslResourceProvider);

        Map<String, String> providerConfiguration = new LinkedHashMap<>();
        providerConfiguration.put("secrets.provider", businessSslResourceProvider);
        providerConfiguration.put("secrets.projectId", configuration.evaluateToString("data.collector.sslBundle.gcp.projectId"));
        String gcsServiceAccountKeyPath = configuration.evaluateToString("data.collector.sslBundle.gcp.serviceAccountKeyPath");
        if (gcsServiceAccountKeyPath != null) {
            providerConfiguration.put("secrets.serviceAccountKeyPath", gcsServiceAccountKeyPath);
        }

        this.secretManagerClient = SecretManagerClient.create(providerConfiguration);
    }

    public String getResourceProvider() {
        return businessSslResourceProvider;
    }

    public boolean hasResourceProvider() {
        return hasBusinessSslResourceProvider;
    }


    @Override
    public String getType() {
        return configuration.evaluateToString("data.collector.sslBundle.type");
    }

    @Override
    public String bundleName() {
        return configuration.evaluateToString("data.collector.sslBundle.name");
    }

    @Override
    public char[] publicCertificate() {
        return isPEM() ? safeCharArrayAsUTF8(secretManagerClient.readBytes(configuration.evaluateToString("data.collector.sslBundle.publicCertificate"))) : new char[0];
    }

    @Override
    public char[] privateCertificate() {
        return isPEM() ? safeCharArrayAsUTF8(secretManagerClient.readBytes(configuration.evaluateToString("data.collector.sslBundle.privateCertificate"))) : new char[0];
    }

    @Override
    public byte[] archiveCertificate() {
        return !isPEM() ? secretManagerClient.readBytes(configuration.evaluateToString("data.collector.sslBundle.archiveCertificate")) : new byte[0];
    }

    @Override
    public char[] passphrase() {
        return safeCharArrayAsUTF8(secretManagerClient.readBytes(configuration.evaluateToString("data.collector.sslBundle.passphrase")));
    }

    @Override
    public void close() {
        secretManagerClient.close();
    }
}
