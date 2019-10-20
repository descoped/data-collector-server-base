package no.ssb.dc.application.controller;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.application.ApplicationException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

class HealthResourceFactory {

    private final List<HealthResource> healthResources;

    private HealthResourceFactory() {
        TreeSet<Class<? extends HealthResource>> resourceClasses = loadHealthResources();
        healthResources = createHealthResources(resourceClasses);
    }

    static List<HealthResource> getInstances() {
        return HealthControllerFactorySingleton.INSTANCE.healthResources;
    }

    private TreeSet<Class<? extends HealthResource>> loadHealthResources() {
        TreeSet<Class<? extends HealthResource>> classes = new TreeSet<>(new HealthResourcePriorityComparator());
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .whitelistPackages("no.ssb.dc")
                .scan()
        ) {
            ClassInfoList classInfoList = scanResult.getClassesImplementing(HealthResource.class.getName());
            classInfoList.forEach(ci -> {
                classes.add((Class<? extends HealthResource>) ci.loadClass());
            });
        }
        return classes;
    }

    private List<HealthResource> createHealthResources(TreeSet<Class<? extends HealthResource>> loadedHealthResources) {
        List<HealthResource> healthResourceArrayList= new ArrayList<>();

        for (Class<? extends HealthResource> resourceClass : loadedHealthResources) {
            try {
                HealthResource healthResource = resourceClass.getDeclaredConstructor().newInstance();
                healthResourceArrayList.add(healthResource);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new ApplicationException(e);
            }
        }

        return healthResourceArrayList;
    }

    private static class HealthControllerFactorySingleton {
        private static final HealthResourceFactory INSTANCE = new HealthResourceFactory();
    }

    static class HealthResourcePriorityComparator implements Comparator<Class<? extends HealthResource>> {

        @Override
        public int compare(Class<? extends HealthResource> o1, Class<? extends HealthResource> o2) {
            if (!o1.isAnnotationPresent(HealthRenderPriority.class)) {
                throw new ApplicationException("HealthResource " + o1 + " must be annotated with " + HealthRenderPriority.class.getName());
            }

            if (!o2.isAnnotationPresent(HealthRenderPriority.class)) {
                throw new ApplicationException("HealthResource " + o2 + " must be annotated with " + HealthRenderPriority.class.getName());
            }

            int o1Priority = o1.getAnnotation(HealthRenderPriority.class).priority();
            int o2Priority = o2.getAnnotation(HealthRenderPriority.class).priority();

            return Integer.compare(o1Priority, o2Priority);
        }
    }
}
