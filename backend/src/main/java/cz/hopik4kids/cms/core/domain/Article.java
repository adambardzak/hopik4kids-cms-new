package cz.hopik4kids.cms.core.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * News post (prd §3B.6). The <b>only</b> entity with a draft/publish workflow:
 * {@code publishedAt == null} means draft.
 */
@Entity
@Table(name = "article")
public class Article extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "text")
    private String excerpt;

    @Column(columnDefinition = "text")
    private String content;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_id")
    private Media cover;

    /** null = draft. */
    @Column
    private Instant publishedAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Media getCover() {
        return cover;
    }

    public void setCover(Media cover) {
        this.cover = cover;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
