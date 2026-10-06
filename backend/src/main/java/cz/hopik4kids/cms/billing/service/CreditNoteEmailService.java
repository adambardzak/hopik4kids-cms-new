package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.domain.CreditNote;
import cz.hopik4kids.cms.kernel.email.EmailService;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;

/** Sends a credit-note (dobropis) PDF to the payer by email. */
@Service
public class CreditNoteEmailService {

    private final CreditNoteService creditNotes;
    private final CreditNotePdfService pdf;
    private final SupplierSettingsService supplier;
    private final EmailService email;
    private final AuditService audit;

    public CreditNoteEmailService(CreditNoteService creditNotes,
                                  CreditNotePdfService pdf,
                                  SupplierSettingsService supplier,
                                  EmailService email,
                                  AuditService audit) {
        this.creditNotes = creditNotes;
        this.pdf = pdf;
        this.supplier = supplier;
        this.email = email;
        this.audit = audit;
    }

    public void send(String creditNoteId) {
        send(creditNoteId, null);
    }

    /** @param testTo when set, sends there (subject prefixed TEST) instead of the payer. */
    public void send(String creditNoteId, String testTo) {
        CreditNote cn = creditNotes.find(creditNoteId);
        if (testTo == null && (cn.getPayerEmail() == null || cn.getPayerEmail().isBlank())) {
            throw ApiException.badRequest("NO_PAYER_EMAIL", "Dobropis nemá e-mail plátce");
        }

        byte[] bytes = pdf.build(creditNoteId);
        String supplierName = supplier.getOrDefault().getName();
        String sender = supplierName == null || supplierName.isBlank() ? "Hopík4Kids" : supplierName;

        String subject = (testTo != null ? "TEST – " : "") + "Dobropis č. " + cn.getNumber() + " — " + sender;
        String body = """
                Dobrý den,

                v příloze zasíláme dobropis č. %s k faktuře č. %s na částku %d Kč.

                Děkujeme,
                %s
                """.formatted(cn.getNumber(), cn.getInvoiceNumber(), cn.getTotalAmount(), sender);

        boolean ok = email.sendWithAttachment(
                testTo != null ? testTo : cn.getPayerEmail(),
                subject,
                body,
                "dobropis-" + cn.getNumber() + ".pdf",
                bytes,
                "application/pdf");

        if (!ok) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "EMAIL_FAILED", "Dobropis se nepodařilo odeslat (zkontrolujte nastavení e-mailu)");
        }
        audit.record("credit-note-email", "CreditNote", creditNoteId,
                "{\"to\":\"" + cn.getPayerEmail() + "\"}");
    }
}
