package com.tt.tripbook.dto;

import java.util.List;
import org.springframework.data.domain.Page;

// Stable JSON shape for paginated endpoints.
// Returning Spring's Page directly is discouraged: its JSON format is not a supported contract.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
