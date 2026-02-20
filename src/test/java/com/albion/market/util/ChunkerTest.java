package com.albion.market.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkerTest {
    @Test
    void chunksListCorrectly() {
        List<Integer> source = List.of(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = Chunker.chunk(source, 3);
        assertEquals(3, chunks.size());
        assertEquals(List.of(1, 2, 3), chunks.get(0));
        assertEquals(List.of(4, 5, 6), chunks.get(1));
        assertEquals(List.of(7), chunks.get(2));
    }
}
