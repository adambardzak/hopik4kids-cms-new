package cz.hopik4kids.cms.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.hopik4kids.cms.billing.domain.Invoice;
import cz.hopik4kids.cms.billing.domain.InvoiceStatus;
import cz.hopik4kids.cms.billing.repository.InvoiceRepository;
import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Invoice lifecycle (prd §6A.5): create from a registration, mark paid/cancelled. */
@Service
public class InvoiceService {

    /** Shirt surcharge, mirrors RegistrationService.SHIRT_PRICE. */
    private static final int SHIRT_PRICE = 500;

    private final InvoiceRepository invoices;
    private final InvoiceNumberService numbers;
    private final RegistrationRepository registrations;
    private final SupplierSettingsService supplier;
    private final AuditService audit;
    private final ObjectMapper json = new ObjectMapper();

    public InvoiceService(InvoiceRepository invoices,
                          InvoiceNumberService numbers,
                          RegistrationRepository registrations,
                          SupplierSettingsService supplier,
                          AuditService audit) {
        this.invoices = invoices;
        this.numbers = numbers;
        this.registrations = registrations;
        this.supplier = supplier;
        this.audit = audit;
    }

    public record Item(String label, int qty, int unitPrice) {
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> list() {
        return invoices.findAllByOrderByIssueDateDescInvoiceNumberDesc().stream()
                .map(this::dtoOf).toList();
    }

    /** Filtered list for the invoice table/export (prd todo #2). All filters optional. */
    @Transactional(readOnly = true)
    public List<InvoiceDto> list(LocalDate from, LocalDate to, String status, String type) {
        InvoiceStatus st = parseStatus(status);
        return invoices.findAllByOrderByIssueDateDescInvoiceNumberDesc().stream()
                .filter(i -> from == null || !i.getIssueDate().isBefore(from))
                .filter(i -> to == null || !i.getIssueDate().isAfter(to))
                .filter(i -> st == null || i.getStatus() == st)
                .filter(i -> type == null || type.isBlank() || type.equalsIgnoreCase(i.getType()))
                .map(this::dtoOf).toList();
    }

    private static InvoiceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return InvoiceStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Whether the given registration already has an invoice (idempotency check for bulk issuing). */
    @Transactional(readOnly = true)
    public boolean existsForRegistration(String registrationId) {
        return invoices.findByRegistrationId(registrationId).isPresent();
    }

    /** Build a DTO with the program vs. shirt amount split derived from the invoice items. */
    public InvoiceDto dtoOf(Invoice i) {
        int shirt = 0;
        for (Item it : readItems(i.getItems())) {
            if (it.label() != null && it.label().toLowerCase().contains("dres")) {
                shirt += it.qty() * it.unitPrice();
            }
        }
        int program = i.getTotalAmount() - shirt;
        return InvoiceDto.from(i, program, shirt);
    }

    @Transactional(readOnly = true)
    public InvoiceDto get(String id) {
        return dtoOf(find(id));
    }

    /** Create an invoice for a registration (idempotent — returns existing if already invoiced). */
    @Transactional
    public InvoiceDto createFromRegistration(String registrationId) {
        Invoice existing = invoices.findByRegistrationId(registrationId).orElse(null);
        if (existing != null) {
            return InvoiceDto.from(existing);
        }

        Registration reg = registrations.findById(registrationId)
                .orElseThrow(() -> ApiException.notFound("Registrace nenalezena"));
        var child = reg.getChild();
        var parent = child.getParent();
        var program = reg.getProgram();

        List<Item> items = new ArrayList<>();
        int programPrice = reg.getPriceSnapshot() - (reg.isWantsShirt() ? SHIRT_PRICE : 0);
        items.add(new Item(program.getName() + " — " + child.getFullName(), 1, programPrice));
        if (reg.isWantsShirt()) {
            items.add(new Item("Dres Hopík", 1, SHIRT_PRICE));
        }

        int dueDays = supplier.getOrDefault().getDefaultDueDays();
        LocalDate issue = LocalDate.now();
        String number = numbers.next();

        Invoice inv = new Invoice();
        inv.setInvoiceNumber(number);
        inv.setRegistrationId(registrationId);
        inv.setType(program.getType().name().toLowerCase());
        inv.setPayerName(parent.getName());
        inv.setPayerAddress(child.getAddress());
        inv.setPayerEmail(parent.getEmail());
        inv.setItems(writeItems(items));
        inv.setTotalAmount(reg.getPriceSnapshot());
        inv.setIssueDate(issue);
        inv.setDueDate(issue.plusDays(dueDays));
        inv.setVariableSymbol(number.replaceAll("[^0-9]", ""));
        inv.setStatus(InvoiceStatus.UNPAID);
        inv = invoices.save(inv);

        audit.record("invoice-create", "Invoice", inv.getId(), "{\"number\":\"" + number + "\"}");
        return InvoiceDto.from(inv);
    }

    @Transactional
    public InvoiceDto markPaid(String id) {
        Invoice inv = find(id);
        inv.setStatus(InvoiceStatus.PAID);
        inv.setPaidAt(Instant.now());
        invoices.save(inv);
        // Keep the registration payment status in sync.
        registrations.findById(inv.getRegistrationId()).ifPresent(r -> {
            r.setPaymentStatus(PaymentStatus.PAID);
            registrations.save(r);
        });
        audit.record("invoice-paid", "Invoice", id);
        return InvoiceDto.from(inv);
    }

    @Transactional
    public InvoiceDto cancel(String id) {
        Invoice inv = find(id);
        inv.setStatus(InvoiceStatus.CANCELLED);
        invoices.save(inv);
        audit.record("invoice-cancel", "Invoice", id);
        return InvoiceDto.from(inv);
    }

    Invoice find(String id) {
        return invoices.findById(id).orElseThrow(() -> ApiException.notFound("Faktura nenalezena"));
    }

    public List<Item> readItems(String jsonItems) {
        try {
            return List.of(json.readValue(jsonItems, Item[].class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeItems(List<Item> items) {
        try {
            return json.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("Items serialization failed", e);
        }
    }
}
