package no.ssb.dc.application.health;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.api.health.HealthResourceExclude;
import no.ssb.dc.application.ApplicationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

public class HealthResourceFactory {

    private final List<Class<? extends HealthResource>> healthResourceClasses;
    private final Map<Class<? extends HealthResource>, ? extends HealthResource> healthResources;
    private final Map<UUID, HealthResource> dynamicHealthResources;

    private HealthResourceFactory() {
        healthResourceClasses = loadHealthResources();
        healthResources = createHealthResources(healthResourceClasses);
        dynamicHealthResources = new LinkedHashMap<>();
    }

    public static HealthResourceFactory create() {
        return new HealthResourceFactory();
    }

    public <R extends HealthResource> R getHealthResource(Class<R> healthResourceClass) {
        return healthResourceClass.cast(healthResources.get(healthResourceClass));
    }

    public List<HealthResource> getHealthResources() {
        SortedSet<HealthResource> resourceList = new TreeSet<>(new HealthResourcePriorityComparator());
        resourceList.addAll(healthResources.values());
        resourceList.addAll(dynamicHealthResources.values());
        if (resourceList.size() != (healthResources.size() + dynamicHealthResources.size())) {
            throw new IllegalStateException("Something is wrong with comparator. Elements are lost!");
        }
        return new ArrayList<>(resourceList);
    }

    private List<Class<? extends HealthResource>> loadHealthResources() {
        List<Class<? extends HealthResource>> classes = new ArrayList<>();
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

    private <R extends HealthResource> R createHealthResource(UUID id, Class<R> healthResourceClass) {
        try {
            Constructor<R> constructor = healthResourceClass.getDeclaredConstructor(UUID.class);
            return constructor.newInstance(id);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ApplicationException(e);
        }
    }

    private Map<Class<? extends HealthResource>, HealthResource> createHealthResources(List<Class<? extends HealthResource>> loadedHealthResources) {
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

    public <R extends HealthResource> R addHealthResource(UUID id, Class<R> healthDynamicResourceClass) {
        HealthResource healthResource = createHealthResource(id, healthDynamicResourceClass);
        dynamicHealthResources.put(id, healthResource);
        return (R) healthResource;
    }

    public <R extends HealthResource> R getHealthResource(UUID id) {
        return (R) dynamicHealthResources.get(id);
    }

    public void removeHealthResource(UUID id) {
        dynamicHealthResources.remove(id);
    }

    private static class HealthResourcePriorityComparator implements Comparator<HealthResource> {

        @Override
        public int compare(HealthResource o1, HealthResource o2) {
            if (!o1.getClass().isAnnotationPresent(HealthRenderPriority.class)) {
                throw new ApplicationException("HealthResource " + o1 + " must be annotated with " + HealthRenderPriority.class.getName());
            }

            if (!o2.getClass().isAnnotationPresent(HealthRenderPriority.class)) {
                throw new ApplicationException("HealthResource " + o2 + " must be annotated with " + HealthRenderPriority.class.getName());
            }

            int o1Priority = o1.getClass().getAnnotation(HealthRenderPriority.class).priority();
            int o2Priority = o2.getClass().getAnnotation(HealthRenderPriority.class).priority();

            return o1Priority == o2Priority ? 1 : o1Priority < o2Priority ? -1 : 1;
        }
    }

}
