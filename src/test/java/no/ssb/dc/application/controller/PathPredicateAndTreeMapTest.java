package no.ssb.dc.application.controller;

import no.ssb.dc.api.http.Request;
import org.junit.jupiter.api.Test;

import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PathPredicateAndTreeMapTest {

    @Test
    public void thatUniqueEntriesAreAddedToTreeMap() {
        PathPredicate key0 = PathPredicate.of(0, "/foo", Request.Method.GET);
        PathPredicate key1 = PathPredicate.of(1, "/foo/{bar}", Request.Method.PUT);
        PathPredicate key2 = PathPredicate.of(1, "/foo/{bar}", Request.Method.GET);

        assertEquals(key1, key1);
        assertEquals(key2, key2);
        assertNotEquals(key1, key2);

        NavigableMap<PathPredicate, PathAction> sortedMap = new TreeMap<>();

        sortedMap.computeIfAbsent(key0, key -> PathAction.empty());
        assertEquals(1, sortedMap.size());
        System.out.printf("%s%n", sortedMap);

        sortedMap.computeIfAbsent(key1, key -> PathAction.empty());
        assertEquals(2, sortedMap.size());
        System.out.printf("%s%n", sortedMap);

        sortedMap.computeIfAbsent(key2, key -> PathAction.empty());
        assertEquals(3, sortedMap.size());
        System.out.printf("%s%n", sortedMap);
    }
}
