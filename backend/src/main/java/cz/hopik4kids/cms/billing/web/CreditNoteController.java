package cz.hopik4kids.cms.billing.web;

import cz.hopik4kids.cms.billing.service.CreditNoteEmailService;
import cz.hopik4kids.cms.billing.service.CreditNotePdfService;
import cz.hopik4kids.cms.billing.service.CreditNoteService;
import cz.hopik4kids.cms.billing.web.dto.CreditNoteDto;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Credit notes / dobropisy (own numbering series). Owner/admin/accountant. */
@RestController
@RequestMapping("/admin/api/billing/credit-notes")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
public class CreditNoteController {

    private final CreditNoteService creditNotes;
    private final CreditNotePdfService pdf;
    private final CreditNoteEmailService emailService;

    public CreditNoteController(CreditNoteService creditNotes, CreditNotePdfService pdf,
                                CreditNoteEmailService emailService) {
        this.creditNotes = creditNotes;
        this.pdf = pdf;
        this.emailService = emailService;
    }

    @GetMapping
    public PageResponse<CreditNoteDto> list() {
        return PageResponse.ofAll(creditNotes.list());
    }

    public record CreateRequest(String invoiceId, String reason) {}

    /** Issue a credit note for an invoice (cancels the invoice). Idempotent per invoice. */
    @PostMapping
    public CreditNoteDto create(@RequestBody CreateRequest req) {
        return creditNotes.createFromInvoice(req.invoiceId(), req.reason());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String id) {
        CreditNoteDto cn = creditNotes.get(id);
        byte[] body = pdf.build(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("dobropis-" + cn.number() + ".pdf").build().toString())
                .body(body);
    }

    /** Send the credit-note PDF to the payer by email. */
    @PostMapping("/{id}/send")
    public void send(@PathVariable String id,
                     @org.springframework.web.bind.annotation.RequestParam(required = false) String to) {
        emailService.send(id, to);
    }
}
