package cz.hopik4kids.cms.documents.domain;

import cz.hopik4kids.cms.core.domain.Media;
import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Internal document / handbook for trainers (prd §6A.8 B): rules, methodology, checklists, forms.
 * Either an uploaded file (PDF/image via {@link Media}) or written {@code content} (rich text/HTML).
 */
@Entity
@Table(name = "document")
public class Document extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentCategory category;

    /** Uploaded file (optional — a document can be file-based or text-based). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private Media file;

    /** Written content (optional). */
    @Column(columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentVisibility visibility = DocumentVisibility.TRAINERS;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public void setCategory(DocumentCategory category) {
        this.category = category;
    }

    public Media getFile() {
        return file;
    }

    public void setFile(Media file) {
        this.file = file;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public DocumentVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(DocumentVisibility visibility) {
        this.visibility = visibility;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
