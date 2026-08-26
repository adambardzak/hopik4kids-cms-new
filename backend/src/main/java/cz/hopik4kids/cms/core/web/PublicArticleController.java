package cz.hopik4kids.cms.core.web;

import cz.hopik4kids.cms.core.repository.ArticleRepository;
import cz.hopik4kids.cms.core.web.dto.PublicArticleDto;
import cz.hopik4kids.cms.core.web.dto.SitemapEntryDto;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public, read-only article endpoints (prd §5.2). Only published articles are exposed. */
@RestController
@RequestMapping("/api/articles")
public class PublicArticleController {

    private final ArticleRepository articles;

    public PublicArticleController(ArticleRepository articles) {
        this.articles = articles;
    }

    @GetMapping
    public PageResponse<PublicArticleDto> list() {
        List<PublicArticleDto> items = articles.findPublished().stream()
                .map(PublicArticleDto::from)
                .toList();
        return PageResponse.ofAll(items);
    }

    /** Sitemap feed - declared before /{slug} to avoid path collision. */
    @GetMapping("/sitemap")
    public PageResponse<SitemapEntryDto> sitemap() {
        List<SitemapEntryDto> items = articles.findPublished().stream()
                .map(a -> new SitemapEntryDto(a.getSlug(), a.getPublishedAt()))
                .toList();
        return PageResponse.ofAll(items);
    }

    @GetMapping("/{slug}")
    public PublicArticleDto get(@PathVariable String slug) {
        return articles.findBySlugWithCover(slug)
                .filter(a -> a.getPublishedAt() != null)
                .map(PublicArticleDto::from)
                .orElseThrow(() -> ApiException.notFound("Článek nenalezen"));
    }
}
