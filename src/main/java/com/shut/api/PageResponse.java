package com.shut.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** One page of a list, in the shape every list endpoint of the API returns. {@code page} counts from 0. */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
