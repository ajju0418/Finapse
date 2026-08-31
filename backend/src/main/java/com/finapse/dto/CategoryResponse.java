package com.finapse.dto;

import com.finapse.entity.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String displayName
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDisplayName());
    }
}
