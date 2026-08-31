package cz.hopik4kids.cms.billing.web;

import cz.hopik4kids.cms.billing.service.InvoiceExportService;
import cz.hopik4kids.cms.billing.service.InvoicePdfService;
import cz.hopik4kids.cms.billing.service.InvoiceService;
import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

/** Invoices (prd §6A.5). Owner/admin/accountant. */
@RestController
@RequestMapping("/admin/api/billing/invoices")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
public class InvoiceController {

    private final InvoiceService invoices;
    private final InvoicePdfService pdf;
    private final InvoiceExportService export;
    private final cz.hopik4kids.cms.billing.service.InvoiceEmailService emailService;
    private final cz.hopik4kids.cms.billing.service.BulkInvoiceService bulkService;

    public InvoiceController(InvoiceService invoices, InvoicePdfService pdf,
                             InvoiceExportService export,
                             cz.hopik4kids.cms.billing.service.InvoiceEmailService emailService,
                             cz.hopik4kids.cms.billing.service.BulkInvoiceService bulkService) {
        this.invoices = invoices;
        this.pdf = pdf;
        this.export = export;
        this.bulkService = bulkService;
        this.emailService = emailService;
    }

    @GetMapping
    public PageResponse<InvoiceDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        if (from == null && to == null && (status == null || status.isBlank())
                && (type == null || type.isBlank())) {
            return PageResponse.ofAll(invoices.list());
        }
        return PageResponse.ofAll(invoices.list(from, to, status, type));
    }

    /** Export the (optionally filtered) invoice list as CSV/XLSX (prd todo #2). */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "xlsx") String format) {
        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        byte[] body = xlsx
                ? export.exportXlsx(from, to, status, type)
                : export.exportCsv(from, to, status, type);
        String filename = "faktury." + (xlsx ? "xlsx" : "csv");
        MediaType contentType = xlsx
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv; charset=UTF-8");
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
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

    /**
     * One-time bulk issuing (prd todo #1): create + optionally email invoices for all active
     * registrations that don't have one yet. Owner/admin only. Use dryRun=true first to preview.
     */
    @PostMapping("/bulk-issue")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public cz.hopik4kids.cms.billing.service.BulkInvoiceService.Result bulkIssue(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "true") boolean send) {
        return bulkService.run(dryRun, send);
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
