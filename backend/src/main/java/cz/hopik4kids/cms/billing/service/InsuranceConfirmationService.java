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
        deliver(registrationId, null);
    }

    /** Paid registrations whose confirmation has not been sent yet. */
    @Transactional(readOnly = true)
    public java.util.List<String> pendingRegistrationIds() {
        return invoices.findAll().stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .map(Invoice::getRegistrationId)
                .distinct()
                .filter(id -> registrations.findById(id).map(r -> !r.isInsuranceConfirmationSent()).orElse(false))
                .toList();
    }

    /** Sends a sample (first pending registration) to {@code to}; does not mark anything as sent. */
    @Transactional
    public boolean sendTest(String to) {
        var ids = pendingRegistrationIds();
        return !ids.isEmpty() && deliver(ids.get(0), to);
    }

    /** Sends all pending confirmations to their real recipients. Returns {sent, failed}. */
    @Transactional
    public int[] sendAllPending() {
        int ok = 0, fail = 0;
        for (String id : pendingRegistrationIds()) {
            if (deliver(id, null)) ok++; else fail++;
        }
        return new int[]{ok, fail};
    }

    /** @param testTo when non-null: send there instead of the payer and leave the sent flag untouched. */
    private boolean deliver(String registrationId, String testTo) {
        try {
            Registration reg = registrations.findById(registrationId).orElse(null);
            // Sent to every payer automatically on payment; idempotent via insuranceConfirmationSent.
            if (reg == null || (testTo == null && reg.isInsuranceConfirmationSent())) {
                return false;
            }
            Invoice inv = invoices.findByRegistrationId(registrationId).orElse(null);
            if (inv == null || inv.getStatus() != InvoiceStatus.PAID) {
                return false; // no paid invoice to base the confirmation on
            }
            String to = testTo != null ? testTo : inv.getPayerEmail();
            if (to == null || to.isBlank()) {
                to = reg.getChild().getParent().getEmail();
            }
            if (to == null || to.isBlank()) {
                log.warn("Insurance confirmation requested for registration {} but no e-mail on file", registrationId);
                return false;
            }

            int paidAmount = inv.getPaidAmount() != null ? inv.getPaidAmount() : inv.getTotalAmount();
            byte[] bytes = pdf.build(inv, reg, paidAmount);

            String sender = supplier.getOrDefault().getName();
            if (sender == null || sender.isBlank()) {
                sender = "Hopík4Kids";
            }
            String childName = reg.getChild() != null ? reg.getChild().getFullName() : "";
            String programName = reg.getProgram() != null ? reg.getProgram().getName() : "kroužek";
            String child = childName.isBlank() ? "dítě" : childName;
            String subject = (testTo != null ? "TEST – " : "") + "Potvrzení o úhradě pro pojišťovnu – " + child + " | Hopík4Kids";
            String body = """
                    Dobrý den, přijali jsme vaši platbu za kroužek pro %s (program: %s). Vše je v pořádku vyřízeno a místo na kroužku je plně rezervované!

                    V příloze tohoto e-mailu najdete potvrzení o úhradě (doklad o zaplacení). Ten obsahuje všechny potřebné náležitosti, takže ho můžete přímo vytisknout nebo přiložit k online žádosti o finanční příspěvek na sportovní aktivitu u Vaší zdravotní pojišťovny. Pokud byste na potvrzení potřebovali cokoliv upravit nebo doplnit, stačí nám odpovědět na tento e-mail a rádi Vám pomůžeme!

                    Moc se těšíme na první lekci a na společné sportování!

                    S pozdravem,

                    Tým Hopík4Kids s.r.o.

                    +420 730 634 153
                    """.formatted(child, programName);

            boolean ok = email.sendWithAttachment(
                    to,
                    subject,
                    body,
                    "potvrzeni-o-platbe-" + inv.getInvoiceNumber() + ".pdf",
                    bytes,
                    "application/pdf");

            if (ok && testTo == null) {
                reg.setInsuranceConfirmationSent(true);
                registrations.save(reg);
                audit.record("insurance-confirmation-email", "Registration", registrationId);
            } else if (!ok) {
                log.warn("Failed to e-mail insurance confirmation for registration {}", registrationId);
            }
            return ok;
        } catch (Exception e) {
            log.error("Insurance confirmation send failed for registration {}: {}", registrationId, e.getMessage());
            return false;
        }
    }
}
