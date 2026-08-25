package cz.hopik4kids.cms.kernel.web;

import java.util.List;

/** Collection envelope per prd §5.1: {@code { items, total, page, pageSize }}. */
public record PageResponse<T>(List<T> items, long total, int page, int pageSize) {

    public static <T> PageResponse<T> ofAll(List<T> items) {
        return new PageResponse<>(items, items.size(), 1, items.size());
    }
}
