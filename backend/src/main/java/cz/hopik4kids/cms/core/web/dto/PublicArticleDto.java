package cz.hopik4kids.cms.core.web.dto;

import cz.hopik4kids.cms.core.domain.Article;

import java.time.Instant;

/** Public article view (prd §5.2). Cover returns full URL + alt (no media prefix hack). */
public record PublicArticleDto(
        String id,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverUrl,
        String coverAlt,
        Instant publishedAt
) {
    public static PublicArticleDto from(Article a) {
        return new PublicArticleDto(
                a.getId(),
                a.getTitle(),
                a.getSlug(),
                a.getExcerpt(),
                a.getContent(),
                a.getCover() == null ? null : a.getCover().getUrl(),
                a.getCover() == null ? null : a.getCover().getAlt(),
                a.getPublishedAt()
        );
    }
}
