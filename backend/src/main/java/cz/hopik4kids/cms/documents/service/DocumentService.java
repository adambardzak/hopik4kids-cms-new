package cz.hopik4kids.cms.documents.service;

import cz.hopik4kids.cms.core.domain.Media;
import cz.hopik4kids.cms.core.repository.MediaRepository;
import cz.hopik4kids.cms.documents.domain.Document;
import cz.hopik4kids.cms.documents.domain.DocumentCategory;
import cz.hopik4kids.cms.documents.domain.DocumentVisibility;
import cz.hopik4kids.cms.documents.repository.DocumentRepository;
import cz.hopik4kids.cms.documents.web.dto.DocumentDto;
import cz.hopik4kids.cms.documents.web.dto.DocumentRequest;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.EnumParser;
import cz.hopik4kids.cms.kernel.web.SecurityUtils;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Internal documents / handbooks (prd §6A.8 B). */
@Service
public class DocumentService {

    private final DocumentRepository documents;
    private final MediaRepository media;
    private final AuditService audit;

    public DocumentService(DocumentRepository documents, MediaRepository media, AuditService audit) {
        this.documents = documents;
        this.media = media;
        this.audit = audit;
    }

    /** Documents visible to the current user. Trainers see TRAINERS-visible only; admins see all. */
    @Transactional(readOnly = true)
    public List<DocumentDto> listVisible() {
        List<Document> docs = SecurityUtils.isPrivileged()
                ? documents.findAllOrdered()
                : documents.findByVisibilityOrdered(DocumentVisibility.TRAINERS);
        return docs.stream().map(DocumentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> listAll() {
        return documents.findAllOrdered().stream().map(DocumentDto::from).toList();
    }

    @Transactional
    public DocumentDto create(DocumentRequest req) {
        Document d = new Document();
        apply(d, req, true);
        d = documents.save(d);
        audit.record("create", "Document", d.getId());
        return DocumentDto.from(d);
    }

    @Transactional
    public DocumentDto update(String id, DocumentRequest req) {
        Document d = find(id);
        apply(d, req, false);
        d = documents.save(d);
        audit.record("update", "Document", d.getId());
        return DocumentDto.from(d);
    }

    @Transactional
    public void delete(String id) {
        documents.delete(find(id));
        audit.record("delete", "Document", id);
    }

    private Document find(String id) {
        return documents.findById(id).orElseThrow(() -> ApiException.notFound("Dokument nenalezen"));
    }

    private void apply(Document d, DocumentRequest req, boolean isCreate) {
        if (req.title() != null) {
            d.setTitle(req.title());
        }
        if (isCreate && (d.getTitle() == null || d.getTitle().isBlank())) {
            throw ApiException.badRequest("MISSING_TITLE", "Název je povinný");
        }
        if (req.category() != null) {
            d.setCategory(EnumParser.parse(DocumentCategory.class, req.category(), "category"));
        } else if (isCreate) {
            d.setCategory(DocumentCategory.OSTATNI);
        }
        if (req.visibility() != null) {
            d.setVisibility(EnumParser.parse(DocumentVisibility.class, req.visibility(), "visibility"));
        } else if (isCreate) {
            d.setVisibility(DocumentVisibility.TRAINERS);
        }
        d.setContent(req.content());
        if (req.sortOrder() != null) {
            d.setSortOrder(req.sortOrder());
        }
        if (req.fileId() != null && !req.fileId().isBlank()) {
            Media file = media.findById(req.fileId())
                    .orElseThrow(() -> ApiException.badRequest("INVALID_FILE", "Soubor nenalezen"));
            d.setFile(file);
        } else {
            d.setFile(null);
        }
        if ((d.getContent() == null || d.getContent().isBlank()) && d.getFile() == null) {
            throw ApiException.badRequest("EMPTY_DOCUMENT", "Dokument musí mít soubor nebo text");
        }
    }
}
