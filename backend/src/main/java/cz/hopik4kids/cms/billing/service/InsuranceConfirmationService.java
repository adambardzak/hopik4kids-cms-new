package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.domain.Invoice;
import cz.hopik4kids.cms.billing.domain.InvoiceStatus;
import cz.hopik4kids.cms.billing.repository.InvoiceRepository;
import cz.hopik4kids.cms.kernel.email.EmailService;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends the health-insurance payment confirmation to a parent once their registration is paid,
 * but only if they requested it on the sign-up form. Idempotent — a confirmation is sent at most
 * once per registration (tracked by {@code insuranceConfirmationSent}). Best-effort: an email
 * failure does not roll back the payment, but leaves the flag unset so it can be retried.
 */
@Service
public class InsuranceConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(InsuranceConfirmationService.class);

    private final RegistrationRepository registrations;
    private final InvoiceRepository invoices;
    private final InsuranceConfirmationPdfService pdf;
    private final SupplierSettingsService supplier;
    private final EmailService email;
    private final AuditService audit;

    public InsuranceConfirmationService(RegistrationRepository registrations,
                                        InvoiceRepository invoices,
                                        InsuranceConfirmationPdfService pdf,
                                        SupplierSettingsService supplier,
                                        EmailService email,
                                        AuditService audit) {
        this.registrations = registrations;
        this.invoices = invoices;
        this.pdf = pdf;
        this.supplier = supplier;
        this.email = email;
        this.audit = audit;
    }

    /**
     * Send the confirmation for a paid registration if the parent requested it and it hasn't been
     * sent yet. Safe to call from any mark-paid path. Swallows errors (logs them) so it never
     * breaks the payment flow.
     */
    @Transactional
    public void sendIfRequested(String registrationId) {
        try {
            Registration reg = registrations.findById(registrationId).orElse(null);
            if (reg == null || !reg.isWantsInsuranceConfirmation() || reg.isInsuranceConfirmationSent()) {
                return;
            }
            Invoice inv = invoices.findByRegistrationId(registrationId).orElse(null);
            if (inv == null || inv.getStatus() != InvoiceStatus.PAID) {
                return; // no paid invoice to base the confirmation on
            }
            String to = inv.getPayerEmail();
            if (to == null || to.isBlank()) {
                to = reg.getChild().getParent().getEmail();
            }
            if (to == null || to.isBlank()) {
                log.warn("Insurance confirmation requested for registration {} but no e-mail on file", registrationId);
                return;
            }

            int paidAmount = inv.getPaidAmount() != null ? inv.getPaidAmount() : inv.getTotalAmount();
            byte[] bytes = pdf.build(inv, reg, paidAmount);

            String sender = supplier.getOrDefault().getName();
            if (sender == null || sender.isBlank()) {
                sender = "Hopík4Kids";
            }
            String childName = reg.getChild() != null ? reg.getChild().getFullName() : "";
            String subject = "Potvrzení o zaplacení pro pojišťovnu — " + sender;
            String body = """
                    Dobrý den,

                    v příloze zasíláme potvrzení o zaplacení úhrady za %s, které si můžete uplatnit
                    u své zdravotní pojišťovny (řada pojišťoven přispívá na sportovní aktivity dětí).

                    Děkujeme,
                    %s
                    """.formatted(
                    childName.isBlank() ? "sportovní kroužek" : childName,
                    sender);

            boolean ok = email.sendWithAttachment(
                    to,
                    subject,
                    body,
                    "potvrzeni-pojistovna-" + inv.getInvoiceNumber() + ".pdf",
                    bytes,
                    "application/pdf");

            if (ok) {
                reg.setInsuranceConfirmationSent(true);
                registrations.save(reg);
                audit.record("insurance-confirmation-email", "Registration", registrationId);
            } else {
                log.warn("Failed to e-mail insurance confirmation for registration {}", registrationId);
            }
        } catch (Exception e) {
            log.error("Insurance confirmation send failed for registration {}: {}", registrationId, e.getMessage());
        }
    }
}
