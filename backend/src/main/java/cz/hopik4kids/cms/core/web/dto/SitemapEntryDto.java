package cz.hopik4kids.cms.core.web.dto;

import java.time.Instant;

/** Sitemap entry (prd §5.2, §4.8). */
public record SitemapEntryDto(String slug, Instant publishedAt) {
}
