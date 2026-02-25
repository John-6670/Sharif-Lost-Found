package com.nexus.nexus.Service;

import java.util.List;

public record ProductPage<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext
) {
}
