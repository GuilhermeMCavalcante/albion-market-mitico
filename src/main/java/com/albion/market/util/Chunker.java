package com.albion.market.util;

import java.util.ArrayList;
import java.util.List;

public final class Chunker {
    private Chunker() {
    }

    public static <T> List<List<T>> chunk(List<T> items, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Chunk size must be > 0");
        }
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            chunks.add(items.subList(i, Math.min(i + size, items.size())));
        }
        return chunks;
    }
}
