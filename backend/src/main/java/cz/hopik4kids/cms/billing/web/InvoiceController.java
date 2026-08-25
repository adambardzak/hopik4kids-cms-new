package cz.hopik4kids.cms.billing.web;

import cz.hopik4kids.cms.billing.service.InvoicePdfService;
import cz.hopik4kids.cms.billing.service.InvoiceService;
import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Invoices (prd §6A.5). Owner/admin/accountant. */
@RestController
@RequestMapping("/admin/api/billing/invoices")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
public class InvoiceController {

    private final InvoiceService invoices;
    private final InvoicePdfService pdf;
    private final cz.hopik4kids.cms.billing.service.InvoiceEmailService emailService;

    public InvoiceController(InvoiceService invoices, InvoicePdfService pdf,
                             cz.hopik4kids.cms.billing.service.InvoiceEmailService emailService) {
        this.invoices = invoices;
        this.pdf = pdf;
        this.emailService = emailService;
    }

    @GetMapping
    public PageResponse<InvoiceDto> list() {
        return PageResponse.ofAll(invoices.list());
    }

    @GetMapping("/{id}")
    public InvoiceDto get(@PathVariable String id) {
        return invoices.get(id);
    }

    /** Create an invoice from a registration (idempotent). */
    @PostMapping
    public InvoiceDto create(@RequestParam String registration) {
        return invoices.createFromRegistration(registration);
    }

    @PostMapping("/{id}/paid")
    public InvoiceDto markPaid(@PathVariable String id) {
        return invoices.markPaid(id);
    }

    @PostMapping("/{id}/cancel")
    public InvoiceDto cancel(@PathVariable String id) {
        return invoices.cancel(id);
    }

    /** Send the invoice PDF to the payer by email. */
    @PostMapping("/{id}/send")
    public void send(@PathVariable String id) {
        emailService.send(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String id) {
        InvoiceDto inv = invoices.get(id);
        byte[] body = pdf.build(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("faktura-" + inv.invoiceNumber() + ".pdf").build().toString())
                .body(body);
    }
}
