package no.ssb.dc.application;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

class ServiceProviderDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceProviderDiscovery.class);

    private ServiceProviderDiscovery() {
    }

    static <T> Class<T> classForName(String clazz) {
        try {
            return (Class<T>) Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new ApplicationException(e);
        }
    }

    static <T> Iterable<Class<T>> discover(Class<T> serviceProviderClass) {
        List<Class<T>> serviceList = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph().whitelistPathsNonRecursive("META-INF/services").scan()) {
            LOG.trace("serviceProviderClass: {}", serviceProviderClass.getName());
            scanResult.getResourcesWithLeafName(serviceProviderClass.getName()).forEachByteArray((Resource res, byte[] content) -> {
                try {
                    try (BufferedReader reader = new BufferedReader(new StringReader(new String(content)))) {
                        reader.lines().forEach(clazz -> serviceList.add(classForName(clazz)));
                    }
                } catch (IOException e) {
                    throw new ApplicationException(e);
                }
            });
        }
        // TODO check for duplicate contextPaths
        return serviceList;
    }
}
