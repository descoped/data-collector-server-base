package io.descoped.dc.application.controller;

import io.descoped.dc.api.health.HealthResource;
import io.descoped.dc.application.health.HealthModuleInfoResource;
import io.descoped.dc.application.health.HealthResourceFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HealthModuleInfoResourceControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(HealthModuleInfoResourceControllerTest.class);

    @Test
    public void thatModuleListIsEmptyUntilModuleSystemCanBeInitializedInTests() {
        HealthModuleInfoResource healthModuleInfoResource = new HealthModuleInfoResource();
        assertEquals(((List) healthModuleInfoResource.resource()).size(), 6);
        System.out.printf("%s -> %s%n", healthModuleInfoResource.name(), healthModuleInfoResource.resource());
    }

    @Test
    public void thatHealthResourcesAreDiscovered() {
        List<HealthResource> healthResources = HealthResourceFactory.create().getHealthResources();
        healthResources.forEach(healthResource -> System.out.printf("%s%n", healthResource));
    }

    @Test
    public void testThreadGroups() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();

        SortedMap<String, List<Thread>> threadGroups = new TreeMap<>(Comparator.comparing(String::toString));
        for (Thread thread : threads) {
            if (thread.getThreadGroup() == null) {
                LOG.warn("ThreadGroup is NULL for thread: {}", thread);
                continue;
            }
            String threadGroupName = thread.getThreadGroup().getName();
            List<Thread> threadList = threadGroups.computeIfAbsent(threadGroupName, list -> new ArrayList<>());
            List<StackTraceElement> stackTraceList = List.of(thread.getStackTrace());
            for (StackTraceElement stackTraceElement : stackTraceList) {
                System.out.printf("st: %s%n", stackTraceElement);
            }

            threadList.add(thread);
            threadGroups.put(threadGroupName, threadList);
        }

        for (Map.Entry<String, List<Thread>> threadEntry : threadGroups.entrySet()) {
            System.out.printf("%s -> %s%n", threadEntry.getKey(), threadEntry.getValue().stream().map(Thread::getName).collect(Collectors.joining(", ")));
        }
    }
}
