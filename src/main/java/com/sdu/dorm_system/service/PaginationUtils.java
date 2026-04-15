package com.sdu.dorm_system.service;

import com.sdu.dorm_system.exception.BusinessException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PaginationUtils() {
    }

    public static Pageable pageable(Integer page, Integer size, Sort sort) {
        int normalizedPage = page == null ? DEFAULT_PAGE : page;
        int normalizedSize = size == null ? DEFAULT_SIZE : size;

        if (normalizedPage < 0) {
            throw BusinessException.badRequest("Page must be greater than or equal to 0");
        }

        if (normalizedSize < 1 || normalizedSize > MAX_SIZE) {
            throw BusinessException.badRequest("Size must be between 1 and " + MAX_SIZE);
        }

        return PageRequest.of(normalizedPage, normalizedSize, sort);
    }

    public static <T> Page<T> pageList(List<T> items, Pageable pageable) {
        if (items == null || items.isEmpty()) {
            return Page.empty(pageable);
        }

        int start = (int) pageable.getOffset();
        if (start >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }

        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }
}
