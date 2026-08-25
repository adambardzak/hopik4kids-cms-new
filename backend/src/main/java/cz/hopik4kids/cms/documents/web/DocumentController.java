package cz.hopik4kids.cms.documents.web;

import cz.hopik4kids.cms.documents.service.DocumentService;
import cz.hopik4kids.cms.documents.web.dto.DocumentDto;
import cz.hopik4kids.cms.documents.web.dto.DocumentRequest;
import cz.hopik4kids.cms.kernel.web.PageResponse;
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

/** Internal documents (prd §6A.8 B). Reading: owner/admin/trainer (scoped by visibility). Writing: owner/admin. */
@RestController
@RequestMapping("/admin/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    /** List documents visible to the current user (trainers get TRAINERS-visible only). */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    public PageResponse<DocumentDto> list() {
        return PageResponse.ofAll(service.listVisible());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DocumentDto create(@RequestBody DocumentRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public DocumentDto update(@PathVariable String id, @RequestBody DocumentRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
