package com.github.kv20230.backend.model.dto;

import java.util.List;

/**
 * One page of a list endpoint.
 *
 * @param items      the entries on this page (empty when {@code page} is past the end)
 * @param page       1-based page number that was requested
 * @param size       requested page size
 * @param totalItems number of entries across all pages
 * @param totalPages number of pages; at least 1 so "page x of y" always renders
 */
public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
