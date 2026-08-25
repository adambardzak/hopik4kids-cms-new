package cz.hopik4kids.cms.core.web.dto;

import cz.hopik4kids.cms.core.domain.Article;

import java.time.Instant;

public record AdminArticleDto(
        String id,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverId,
        String coverUrl,
        Instant publishedAt,
        boolean published
) {
    public static AdminArticleDto from(Article a) {
        return new AdminArticleDto(
                a.getId(),
                a.getTitle(),
                a.getSlug(),
                a.getExcerpt(),
                a.getContent(),
                a.getCover() == null ? null : a.getCover().getId(),
                a.getCover() == null ? null : a.getCover().getUrl(),
                a.getPublishedAt(),
                a.getPublishedAt() != null
        );
    }
}
