package no.ssb.dc.application.controller;

import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.application.health.HealthModuleInfoResource;
import no.ssb.dc.application.health.HealthResourceFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

public class HealthModuleInfoResourceControllerTest {

    @Test
    public void thatModuleListIsEmptyUntilModuleSystemCanBeInitializedInTests() {
        HealthModuleInfoResource healthModuleInfoResource = new HealthModuleInfoResource();
        assertEquals(((List) healthModuleInfoResource.resource()).size(), 0);
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
