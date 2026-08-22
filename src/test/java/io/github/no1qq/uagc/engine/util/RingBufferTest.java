package io.github.no1qq.uagc.engine.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingBufferTest {

    @Test
    void retainsOnlyTheMostRecentEntries() {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);
        for (int i = 1; i <= 5; i++) {
            buffer.add(i);
        }
        assertEquals(3, buffer.size());
        assertEquals(List.of(3, 4, 5), buffer.toList());
        assertEquals(5, buffer.last());
        assertEquals(4, buffer.fromEnd(1));
        assertEquals(3, buffer.fromEnd(2));
    }

    @Test
    void reportsCapacityState() {
        RingBuffer<String> buffer = new RingBuffer<>(2);
        assertTrue(buffer.isEmpty());
        assertNull(buffer.lastOrNull());
        buffer.add("a");
        assertFalse(buffer.isFull());
        buffer.add("b");
        assertTrue(buffer.isFull());
        assertEquals(2, buffer.capacity());
    }

    @Test
    void newestFirstIsBoundedByAvailableEntries() {
        RingBuffer<Integer> buffer = new RingBuffer<>(8);
        buffer.add(1);
        buffer.add(2);
        assertEquals(List.of(2, 1), buffer.newestFirst(5));
    }

    @Test
    void clearResetsEverything() {
        RingBuffer<Integer> buffer = new RingBuffer<>(4);
        buffer.add(9);
        buffer.clear();
        assertTrue(buffer.isEmpty());
        assertNull(buffer.fromEndOrNull(0));
    }

    @Test
    void rejectsInvalidAccess() {
        RingBuffer<Integer> buffer = new RingBuffer<>(2);
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(0));
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer<Integer>(0));
    }
}
