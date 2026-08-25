package cz.hopik4kids.cms.core.web;

import cz.hopik4kids.cms.core.domain.Article;
import cz.hopik4kids.cms.core.domain.Media;
import cz.hopik4kids.cms.core.repository.ArticleRepository;
import cz.hopik4kids.cms.core.repository.MediaRepository;
import cz.hopik4kids.cms.core.web.dto.AdminArticleDto;
import cz.hopik4kids.cms.core.web.dto.AdminArticleRequest;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin Article CRUD (prd §5.6, §6.6). Restricted to owner/admin. Only entity with draft/publish. */
@RestController
@RequestMapping("/admin/api/articles")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminArticleController {

    private final ArticleRepository articles;
    private final MediaRepository media;
    private final AuditService audit;

    public AdminArticleController(ArticleRepository articles, MediaRepository media, AuditService audit) {
        this.articles = articles;
        this.media = media;
        this.audit = audit;
    }

    @GetMapping
    public PageResponse<AdminArticleDto> list() {
        return PageResponse.ofAll(articles.findAll().stream().map(AdminArticleDto::from).toList());
    }

    @GetMapping("/{id}")
    public AdminArticleDto get(@PathVariable String id) {
        return AdminArticleDto.from(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminArticleDto create(@RequestBody AdminArticleRequest req) {
        Article a = new Article();
        apply(a, req, true);
        a = articles.save(a);
        audit.record("create", "Article", a.getId());
        return AdminArticleDto.from(a);
    }

    @PutMapping("/{id}")
    public AdminArticleDto update(@PathVariable String id, @RequestBody AdminArticleRequest req) {
        Article a = find(id);
        apply(a, req, false);
        a = articles.save(a);
        audit.record("update", "Article", a.getId());
        return AdminArticleDto.from(a);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        Article a = find(id);
        articles.delete(a);
        audit.record("delete", "Article", id);
    }

    private Article find(String id) {
        return articles.findById(id).orElseThrow(() -> ApiException.notFound("Článek nenalezen"));
    }

    private void apply(Article a, AdminArticleRequest req, boolean isCreate) {
        if (req.title() != null) {
            a.setTitle(req.title());
        }
        if (req.slug() != null) {
            a.setSlug(req.slug());
        }
        if (isCreate && (a.getTitle() == null || a.getSlug() == null)) {
            throw ApiException.badRequest("MISSING_FIELDS", "Titulek a slug jsou povinné");
        }
        // Uniqueness check on slug (excluding self).
        articles.findBySlug(a.getSlug())
                .filter(other -> !other.getId().equals(a.getId()))
                .ifPresent(other -> {
                    throw ApiException.conflict("SLUG_TAKEN", "Slug už existuje");
                });
        a.setExcerpt(req.excerpt());
        a.setContent(req.content());
        a.setPublishedAt(req.publishedAt());
        if (req.coverId() != null && !req.coverId().isBlank()) {
            Media cover = media.findById(req.coverId())
                    .orElseThrow(() -> ApiException.badRequest("INVALID_COVER", "Obrázek nenalezen"));
            a.setCover(cover);
        } else {
            a.setCover(null);
        }
    }
}
