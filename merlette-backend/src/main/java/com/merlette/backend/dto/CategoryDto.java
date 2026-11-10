package com.merlette.backend.dto;

import com.merlette.backend.entity.Category;

public record CategoryDto(Integer id, String name, String slug) {

    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getSlug());
    }
}
