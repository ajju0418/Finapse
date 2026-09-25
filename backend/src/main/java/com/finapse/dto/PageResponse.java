package com.finapse.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination envelope for list endpoints.
 *
 * <p>Spring's {@code PageImpl} serialises to an unstable JSON shape that Spring
 * itself warns against exposing over an API. This record pins the contract the
 * frontend depends on: the page's rows plus the metadata a pager needs.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
