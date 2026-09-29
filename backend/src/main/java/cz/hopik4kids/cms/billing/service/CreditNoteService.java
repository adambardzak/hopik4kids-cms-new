package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.domain.CreditNote;
import cz.hopik4kids.cms.billing.domain.Invoice;
import cz.hopik4kids.cms.billing.domain.InvoiceStatus;
import cz.hopik4kids.cms.billing.repository.CreditNoteRepository;
import cz.hopik4kids.cms.billing.repository.InvoiceRepository;
import cz.hopik4kids.cms.billing.web.dto.CreditNoteDto;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Credit notes (dobropisy). A credit note reverses one invoice in full, has its own yearly number
 * series (D-2026-0001), and snapshots the invoice's payer + items. Issuing one cancels the invoice.
 */
@Service
public class CreditNoteService {

    private final CreditNoteRepository creditNotes;
    private final InvoiceRepository invoices;
    private final CreditNoteNumberService numbers;
    private final RegistrationRepository registrations;
    private final AuditService audit;

    public CreditNoteService(CreditNoteRepository creditNotes,
                             InvoiceRepository invoices,
                             CreditNoteNumberService numbers,
                             RegistrationRepository registrations,
                             AuditService audit) {
        this.creditNotes = creditNotes;
        this.invoices = invoices;
        this.numbers = numbers;
        this.registrations = registrations;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CreditNoteDto> list() {
        return creditNotes.findAllByOrderByIssueDateDescNumberDesc().stream()
                .map(CreditNoteDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CreditNoteDto get(String id) {
        return CreditNoteDto.from(find(id));
    }

    /**
     * Issue a credit note for an invoice (idempotent — returns the existing one if already issued).
     * Snapshots payer/items/amount from the invoice, cancels the invoice, and reopens the linked
     * registration's payment status to UNPAID.
     */
    @Transactional
    public CreditNoteDto createFromInvoice(String invoiceId, String reason) {
        CreditNote existing = creditNotes.findByInvoiceId(invoiceId).orElse(null);
        if (existing != null) {
            return CreditNoteDto.from(existing);
        }

        Invoice inv = invoices.findById(invoiceId)
                .orElseThrow(() -> ApiException.notFound("Faktura nenalezena"));

        CreditNote c = new CreditNote();
        c.setNumber(numbers.next());
        c.setInvoiceId(inv.getId());
        c.setInvoiceNumber(inv.getInvoiceNumber());
        c.setPayerName(inv.getPayerName());
        c.setPayerAddress(inv.getPayerAddress());
        c.setPayerEmail(inv.getPayerEmail());
        c.setItems(inv.getItems());
        c.setTotalAmount(inv.getTotalAmount());
        c.setIssueDate(LocalDate.now());
        c.setVariableSymbol(inv.getVariableSymbol());
        c.setReason(reason);
        c = creditNotes.save(c);

        // Issuing a credit note cancels the invoice.
        inv.setStatus(InvoiceStatus.CANCELLED);
        invoices.save(inv);

        // Reopen the registration's payment status (money is being returned).
        registrations.findById(inv.getRegistrationId()).ifPresent(r -> {
            r.setPaymentStatus(PaymentStatus.UNPAID);
            registrations.save(r);
        });

        audit.record("credit-note-create", "CreditNote", c.getId(),
                "{\"number\":\"" + c.getNumber() + "\",\"invoice\":\"" + inv.getInvoiceNumber() + "\"}");
        return CreditNoteDto.from(c);
    }

    CreditNote find(String id) {
        return creditNotes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Dobropis nenalezen"));
    }
}
