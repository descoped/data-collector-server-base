package no.ssb.dc.application.controller;

import no.ssb.dc.api.health.HealthResource;
import no.ssb.dc.application.health.HealthModuleInfoResource;
import no.ssb.dc.application.health.HealthResourceFactory;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class HealthModuleInfoResourceControllerTest {

    @Test
    public void thatModuleListIsEmptyUntilModuleSystemCanBeInitializedInTests() {
        HealthModuleInfoResource healthModuleInfoResource = new HealthModuleInfoResource();
        assertEquals(((List) healthModuleInfoResource.resource()).size(), 0);
        System.out.printf("%s -> %s%n", healthModuleInfoResource.name(), healthModuleInfoResource.resource());
    }

    @Test
    public void thatHealthResourcesAeDiscovered() {
        List<HealthResource> healthResources = HealthResourceFactory.create().getHealthResources();
        healthResources.forEach(healthResource -> System.out.printf("%s%n", healthResource));
    }

}
