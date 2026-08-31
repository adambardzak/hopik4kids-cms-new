package cz.hopik4kids.cms.records.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.records.domain.RecordDocument;
import cz.hopik4kids.cms.records.service.RecordDocumentService;
import cz.hopik4kids.cms.records.web.dto.RecordDocumentDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Accounting/HR documents (prd todo #8): receipts, DPP agreements, contracts.
 * Sensitive → owner/admin/accountant only. Files stream via an authenticated download endpoint.
 */
@RestController
@RequestMapping("/admin/api/records")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
public class RecordDocumentController {

    private final RecordDocumentService service;

    public RecordDocumentController(RecordDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<RecordDocumentDto> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String person) {
        return PageResponse.ofAll(service.list(type, person));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecordDocumentDto create(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "personId", required = false) String personId,
            @RequestParam(value = "personName", required = false) String personName,
            @RequestParam(value = "docDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate docDate,
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            @RequestParam(value = "note", required = false) String note) {
        return service.create(file, type, title, personId, personName, docDate, amount, note);
    }

    /** Stream the file inline (authenticated). */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        RecordDocument r = service.get(id);
        byte[] body = service.file(r);
        MediaType ct = r.getContentType() != null
                ? MediaType.parseMediaType(r.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        String name = r.getOriginalName() != null ? r.getOriginalName() : "doklad";
        return ResponseEntity.ok()
                .contentType(ct)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(name).build().toString())
                .body(body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
