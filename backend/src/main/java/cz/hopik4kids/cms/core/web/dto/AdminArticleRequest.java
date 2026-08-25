package cz.hopik4kids.cms.core.web.dto;

import java.time.Instant;

/** Admin article create/update (prd §6.6). publishedAt null = draft. */
public record AdminArticleRequest(
        String title,
        String slug,
        String excerpt,
        String content,
        String coverId,
        Instant publishedAt
) {
}
