package cz.hopik4kids.cms.core.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Uploaded media (prd §3B.7). Variants stored as JSON in {@code variants}. */
@Entity
@Table(name = "media")
public class Media extends BaseEntity {

    @Column(nullable = false)
    private String url;

    @Column
    private String alt;

    @Column
    private Integer width;

    @Column
    private Integer height;

    /** JSON of size variants (thumbnail/small/medium/large), stored as text. */
    @Column(columnDefinition = "text")
    private String variants;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getVariants() {
        return variants;
    }

    public void setVariants(String variants) {
        this.variants = variants;
    }
}
