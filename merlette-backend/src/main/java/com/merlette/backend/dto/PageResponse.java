package com.merlette.backend.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Liste paginée : { data, pagination: { page, limit, total, totalPages } }. */
public record PageResponse<T>(List<T> data, Pagination pagination) {

    public record Pagination(int page, int limit, long total, int totalPages) {}

    public static <T> PageResponse<T> of(List<T> data, Page<?> page) {
        return new PageResponse<>(data, new Pagination(
                page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }
}
