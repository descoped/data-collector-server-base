package no.ssb.dc.application.health;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.api.health.HealthResourceExclude;
import no.ssb.dc.application.ApplicationException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class HealthResourceFactory {

    private final TreeSet<Class<? extends HealthResource>> healthResourceClasses;
    private final Map<Class<? extends HealthResource>, ? extends HealthResource> healthResources;

    private HealthResourceFactory() {
        healthResourceClasses = loadHealthResources();
        healthResources = createHealthResources(healthResourceClasses);
    }

    public static HealthResourceFactory create() {
        return new HealthResourceFactory();
    }

    public <R extends HealthResource> R getHealthResource(Class<R> healthResourceClass) {
        return healthResourceClass.cast(healthResources.get(healthResourceClass));
    }

    public List<HealthResource> getHealthResources() {
        List<HealthResource> resourceList = new ArrayList<>();
        resourceList.addAll(healthResources.values());
        return resourceList;
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
                classes.add(ci.loadClass(HealthResource.class));
            });
        }
        return classes;
    }

    private <R extends HealthResource> R createHealthResource(Class<R> healthResourceClass) {
        try {
            return healthResourceClass.getDeclaredConstructor().newInstance();

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ApplicationException(e);
        }
    }

    private Map<Class<? extends HealthResource>, HealthResource> createHealthResources(TreeSet<Class<? extends HealthResource>> loadedHealthResources) {
        Map<Class<? extends HealthResource>, HealthResource> healthResourceMap = new LinkedHashMap<>();

        for (Class<? extends HealthResource> resourceClass : loadedHealthResources) {
            // do not eager instantiate classes marked with excluded
            if (resourceClass.isAnnotationPresent(HealthResourceExclude.class)) {
                continue;
            }

            HealthResource healthResource = createHealthResource(resourceClass);
            healthResourceMap.put(resourceClass, healthResource);
        }

        return healthResourceMap;
    }

    private static class HealthResourcePriorityComparator implements Comparator<Class<? extends HealthResource>> {

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
