package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.kernel.email.EmailService;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;

/** Sends an invoice PDF to the payer by email (prd §6A.5). */
@Service
public class InvoiceEmailService {

    private final InvoiceService invoices;
    private final InvoicePdfService pdf;
    private final SupplierSettingsService supplier;
    private final EmailService email;
    private final AuditService audit;

    public InvoiceEmailService(InvoiceService invoices,
                               InvoicePdfService pdf,
                               SupplierSettingsService supplier,
                               EmailService email,
                               AuditService audit) {
        this.invoices = invoices;
        this.pdf = pdf;
        this.supplier = supplier;
        this.email = email;
        this.audit = audit;
    }

    public void send(String invoiceId) {
        InvoiceDto inv = invoices.get(invoiceId);
        if (inv.payerEmail() == null || inv.payerEmail().isBlank()) {
            throw ApiException.badRequest("NO_PAYER_EMAIL", "Faktura nemá e-mail plátce");
        }

        byte[] bytes = pdf.build(invoiceId);
        String supplierName = supplier.getOrDefault().getName();

        String body = """
                Dobrý den,

                v příloze zasíláme fakturu č. %s na částku %d Kč se splatností %s.
                Fakturu můžete zaplatit převodem (variabilní symbol %s) nebo naskenováním
                QR platby přímo z faktury.

                Děkujeme,
                %s
                """.formatted(
                inv.invoiceNumber(),
                inv.totalAmount(),
                inv.dueDate(),
                inv.variableSymbol(),
                supplierName == null ? "Hopík4Kids" : supplierName);

        boolean ok = email.sendWithAttachment(
                inv.payerEmail(),
                "Faktura č. " + inv.invoiceNumber() + " — " + (supplierName == null ? "Hopík4Kids" : supplierName),
                body,
                "faktura-" + inv.invoiceNumber() + ".pdf",
                bytes,
                "application/pdf");

        if (!ok) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "EMAIL_FAILED", "Fakturu se nepodařilo odeslat (zkontrolujte nastavení e-mailu)");
        }
        audit.record("invoice-email", "Invoice", invoiceId, "{\"to\":\"" + inv.payerEmail() + "\"}");
    }
}
