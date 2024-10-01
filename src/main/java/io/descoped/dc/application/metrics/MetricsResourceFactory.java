package io.descoped.dc.application.metrics;

import io.descoped.dc.api.metrics.MetricsResource;
import io.descoped.dc.application.server.ApplicationException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MetricsResourceFactory {

    private final List<Class<? extends MetricsResource>> metricsResourceClasses;
    private final Map<Class<? extends MetricsResource>, ? extends MetricsResource> metricsResources;

    public MetricsResourceFactory() {
        metricsResourceClasses = loadMetricsResources();
        metricsResources = createMetricsResources(metricsResourceClasses);
    }

    public static MetricsResourceFactory create() {
        return new MetricsResourceFactory();
    }

    private List<Class<? extends MetricsResource>> loadMetricsResources() {
        List<Class<? extends MetricsResource>> classes = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .whitelistPackages("io.descoped.dc")
                .scan()
        ) {
            ClassInfoList classInfoList = scanResult.getClassesImplementing(MetricsResource.class.getName());
            classInfoList.forEach(ci -> {
                classes.add(ci.loadClass(MetricsResource.class));
            });
        }
        return classes;
    }

    private <R extends MetricsResource> R createMetricsResource(Class<R> metricsResourceClass) {
        try {
            return metricsResourceClass.getDeclaredConstructor().newInstance();

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new ApplicationException(e);
        }
    }


    private Map<Class<? extends MetricsResource>, ? extends MetricsResource> createMetricsResources(List<Class<? extends MetricsResource>> loadedMetricsResources) {
        Map<Class<? extends MetricsResource>, MetricsResource> metricsResourceMap = new LinkedHashMap<>();

        for (Class<? extends MetricsResource> resourceClass : loadedMetricsResources) {
            MetricsResource metricsResource = createMetricsResource(resourceClass);
            metricsResourceMap.put(resourceClass, metricsResource);
        }

        return metricsResourceMap;
    }

    public <R extends MetricsResource> R get(Class<R> metricsResourceClass) {
        return (R) metricsResources.get(metricsResourceClass);
    }

}
