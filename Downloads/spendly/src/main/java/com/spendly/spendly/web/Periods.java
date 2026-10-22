package com.spendly.spendly.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

final class Periods {

    static final int PAGE_SIZE = 25;

    private Periods() {
    }

    /** Parses ?month=2026-10, falling back to the current month. */
    static YearMonth month(String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }

    static LocalDate today() {
        return LocalDate.now();
    }

    static Pageable page(int page, Sort sort) {
        return PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort);
    }

    static String q(String q) {
        return q == null ? "" : q.trim();
    }
}
