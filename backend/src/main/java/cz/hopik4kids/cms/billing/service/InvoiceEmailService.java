package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.kernel.email.EmailService;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;

/** Sends an invoice PDF to the payer by email (prd §6A.5). */
@Service
public class InvoiceEmailService {

    private final InvoiceService invoices;
    private final InvoicePdfService pdf;
    private final SupplierSettingsService supplier;
    private final EmailService email;
    private final RegistrationRepository registrations;
    private final AuditService audit;

    public InvoiceEmailService(InvoiceService invoices,
                               InvoicePdfService pdf,
                               SupplierSettingsService supplier,
                               EmailService email,
                               RegistrationRepository registrations,
                               AuditService audit) {
        this.invoices = invoices;
        this.pdf = pdf;
        this.supplier = supplier;
        this.email = email;
        this.registrations = registrations;
        this.audit = audit;
    }

    public void send(String invoiceId) {
        sendInternal(invoiceId, false);
    }

    /**
     * Welcome + invoice email sent automatically right after a parent registers (auto-invoicing).
     * Thank-you copy with the invoice attached. Best-effort caller should catch failures.
     */
    public void sendWelcome(String invoiceId, String childName, String programName) {
        sendInternal(invoiceId, true, childName, programName);
    }

    private void sendInternal(String invoiceId, boolean welcome) {
        sendInternal(invoiceId, welcome, null, null);
    }

    private void sendInternal(String invoiceId, boolean welcome, String childName, String programName) {
        InvoiceDto inv = invoices.get(invoiceId);
        if (inv.payerEmail() == null || inv.payerEmail().isBlank()) {
            throw ApiException.badRequest("NO_PAYER_EMAIL", "Faktura nemá e-mail plátce");
        }

        byte[] bytes = pdf.build(invoiceId);
        String supplierName = supplier.getOrDefault().getName();
        String sender = supplierName == null ? "Hopík4Kids" : supplierName;

        String body;
        String subject;
        if (welcome) {
            subject = "Potvrzení registrace" + (programName != null ? " — " + programName : "") + " | " + sender;
            body = """
                    Dobrý den,

                    děkujeme za přihlášení%s do programu %s. Máme registraci v pořádku zaznamenanou.

                    V příloze najdete fakturu č. %s na částku %d Kč se splatností %s.
                    Zaplatit můžete převodem (variabilní symbol %s) nebo naskenováním QR platby
                    přímo z faktury.

                    Pokud budete mít jakýkoliv dotaz, neváhejte nás kontaktovat.

                    Těšíme se na vás,
                    %s
                    """.formatted(
                    childName != null ? " " + childName : "",
                    programName != null ? programName : "Hopík4Kids",
                    inv.invoiceNumber(),
                    inv.totalAmount(),
                    inv.dueDate(),
                    inv.variableSymbol(),
                    sender);
        } else {
            subject = "Faktura č. " + inv.invoiceNumber() + " — " + sender;
            body = """
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
                    sender);
        }

        boolean ok = email.sendWithAttachment(
                inv.payerEmail(),
                subject,
                body,
                "faktura-" + inv.invoiceNumber() + ".pdf",
                bytes,
                "application/pdf");

        if (!ok) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "EMAIL_FAILED", "Fakturu se nepodařilo odeslat (zkontrolujte nastavení e-mailu)");
        }
        audit.record("invoice-email", "Invoice", invoiceId, "{\"to\":\"" + inv.payerEmail() + "\"}");

        // Reflect that the invoice was sent on the registration's payment status (unless already paid).
        registrations.findById(inv.registrationId()).ifPresent(reg -> {
            if (reg.getPaymentStatus() == PaymentStatus.UNPAID) {
                reg.setPaymentStatus(PaymentStatus.INVOICE_SENT);
                registrations.save(reg);
            }
        });
    }
}
