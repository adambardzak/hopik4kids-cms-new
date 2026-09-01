package cz.hopik4kids.cms.registrations.service;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.billing.domain.Invoice;
import cz.hopik4kids.cms.billing.domain.InvoiceStatus;
import cz.hopik4kids.cms.billing.repository.InvoiceRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.EnumParser;
import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.registrations.domain.RegistrationStatus;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.registrations.web.dto.AdminRegistrationDto;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminRegistrationService {

    private final RegistrationRepository registrations;
    private final ProgramRepository programs;
    private final InvoiceRepository invoices;
    private final AuditService audit;

    public AdminRegistrationService(RegistrationRepository registrations,
                                    ProgramRepository programs,
                                    InvoiceRepository invoices,
                                    AuditService audit) {
        this.registrations = registrations;
        this.programs = programs;
        this.invoices = invoices;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AdminRegistrationDto> list(String programId, String paymentStatus, String q) {
        PaymentStatus ps = EnumParser.parse(PaymentStatus.class, paymentStatus, "paymentStatus");
        List<Registration> rows = registrations.findForAdmin(blankToNull(programId), ps, blankToNull(q));
        // Batch-load invoices to derive overdue + invoiceId without N+1.
        Map<String, Invoice> byReg = invoices
                .findByRegistrationIdIn(rows.stream().map(Registration::getId).toList())
                .stream()
                .collect(Collectors.toMap(Invoice::getRegistrationId, Function.identity(), (a, b) -> a));
        return rows.stream()
                .map(r -> toDto(r, byReg.get(r.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminRegistrationDto get(String id) {
        Registration r = find(id);
        return toDto(r, invoices.findByRegistrationId(id).orElse(null));
    }

    /** Build the DTO with invoice-derived overdue flag + invoice id. */
    private AdminRegistrationDto toDto(Registration r, Invoice inv) {
        boolean overdue = inv != null
                && inv.getStatus() == InvoiceStatus.UNPAID
                && r.getPaymentStatus() != PaymentStatus.PAID
                && r.getPaymentStatus() != PaymentStatus.CANCELLED
                && inv.getDueDate() != null
                && inv.getDueDate().isBefore(LocalDate.now());
        return AdminRegistrationDto.from(r, overdue, inv == null ? null : inv.getId());
    }

    /** Soft-delete (prd §4.9): status=cancelled, decrement spotsTaken transactionally. */
    @Transactional
    public AdminRegistrationDto cancel(String id) {
        Registration r = find(id);
        if (r.getStatus() == RegistrationStatus.CANCELLED) {
            return AdminRegistrationDto.from(r);
        }
        r.setStatus(RegistrationStatus.CANCELLED);
        r.setPaymentStatus(PaymentStatus.CANCELLED);
        registrations.save(r);

        Program program = programs.findByIdForUpdate(r.getProgram().getId()).orElseThrow();
        program.setSpotsTaken(Math.max(0, program.getSpotsTaken() - 1));
        programs.save(program);

        audit.record("cancel", "Registration", id);
        return AdminRegistrationDto.from(r);
    }

    @Transactional
    public AdminRegistrationDto setPaymentStatus(String id, String status) {
        Registration r = find(id);
        PaymentStatus ps = EnumParser.parseRequired(PaymentStatus.class, status, "paymentStatus");
        r.setPaymentStatus(ps);
        registrations.save(r);

        // Keep the linked invoice in sync (two-way): marking a registration paid marks its invoice
        // paid, and reverting to unpaid reopens it. Cancelled invoices are left untouched.
        invoices.findByRegistrationId(id).ifPresent(inv -> {
            if (inv.getStatus() == InvoiceStatus.CANCELLED) {
                return;
            }
            if (ps == PaymentStatus.PAID && inv.getStatus() != InvoiceStatus.PAID) {
                inv.setStatus(InvoiceStatus.PAID);
                inv.setPaidAt(java.time.Instant.now());
                invoices.save(inv);
            } else if ((ps == PaymentStatus.UNPAID || ps == PaymentStatus.INVOICE_SENT)
                    && inv.getStatus() == InvoiceStatus.PAID) {
                inv.setStatus(InvoiceStatus.UNPAID);
                inv.setPaidAt(null);
                invoices.save(inv);
            }
        });

        audit.record("payment-status", "Registration", id, "{\"status\":\"" + ps.name() + "\"}");
        return AdminRegistrationDto.from(r);
    }

    private Registration find(String id) {
        return registrations.findById(id)
                .orElseThrow(() -> ApiException.notFound("Registrace nenalezena"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
