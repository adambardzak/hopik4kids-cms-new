package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time bulk invoicing (prd todo #1): issue + email an invoice for every active registration
 * that doesn't have one yet. For registrations that pre-date auto-invoicing. Idempotent — already
 * invoiced registrations are skipped (createFromRegistration returns the existing invoice, and we
 * only email freshly created ones). Not a scheduled job; triggered manually by an admin.
 */
@Service
public class BulkInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(BulkInvoiceService.class);

    private final RegistrationRepository registrations;
    private final InvoiceService invoiceService;
    private final InvoiceEmailService invoiceEmailService;
    private final AuditService audit;

    public BulkInvoiceService(RegistrationRepository registrations,
                              InvoiceService invoiceService,
                              InvoiceEmailService invoiceEmailService,
                              AuditService audit) {
        this.registrations = registrations;
        this.invoiceService = invoiceService;
        this.invoiceEmailService = invoiceEmailService;
        this.audit = audit;
    }

    public record Result(int candidates, int created, int emailed, int skipped, int failed) {}

    /**
     * @param dryRun when true, only counts what would happen (no invoices created, no emails sent)
     * @param send   when true, emails each newly created invoice to the payer
     */
    @Transactional
    public Result run(boolean dryRun, boolean send) {
        var active = registrations.findAllActiveWithDetails();
        int candidates = 0, created = 0, emailed = 0, skipped = 0, failed = 0;

        for (Registration r : active) {
            boolean hasEmail = r.getChild() != null
                    && r.getChild().getParent() != null
                    && r.getChild().getParent().getEmail() != null
                    && !r.getChild().getParent().getEmail().isBlank();
            boolean paid = r.getPriceSnapshot() > 0;

            // Only paid registrations with a payer email are candidates.
            if (!paid || !hasEmail) {
                skipped++;
                continue;
            }
            candidates++;
            if (dryRun) {
                continue;
            }

            try {
                // Was there already an invoice before this call? (idempotency signal)
                boolean alreadyInvoiced = invoiceService.existsForRegistration(r.getId());
                InvoiceDto inv = invoiceService.createFromRegistration(r.getId());
                if (alreadyInvoiced) {
                    skipped++;
                    continue;
                }
                created++;
                if (send) {
                    invoiceEmailService.send(inv.id());
                    emailed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Bulk invoice failed for registration {}: {}", r.getId(), e.getMessage());
            }
        }

        audit.record("invoice-bulk", "Invoice", null,
                "candidates=" + candidates + " created=" + created + " emailed=" + emailed
                        + " skipped=" + skipped + " failed=" + failed + " dryRun=" + dryRun);
        return new Result(candidates, created, emailed, skipped, failed);
    }
}
