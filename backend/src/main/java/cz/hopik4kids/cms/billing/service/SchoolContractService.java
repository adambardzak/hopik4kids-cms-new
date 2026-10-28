package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.domain.SchoolContract;
import cz.hopik4kids.cms.billing.domain.SchoolPriceUnit;
import cz.hopik4kids.cms.billing.repository.InvoiceRepository;
import cz.hopik4kids.cms.billing.repository.SchoolContractRepository;
import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.billing.web.dto.SchoolContractDto;
import cz.hopik4kids.cms.billing.web.dto.SchoolContractRequest;
import cz.hopik4kids.cms.billing.web.dto.SchoolInvoiceRequest;
import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramType;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.core.service.AdminProgramService;
import cz.hopik4kids.cms.core.web.dto.AdminProgramDto;
import cz.hopik4kids.cms.core.web.dto.AdminProgramRequest;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.EnumParser;
import cz.hopik4kids.cms.scheduling.domain.LessonOverrideType;
import cz.hopik4kids.cms.scheduling.repository.LessonOverrideRepository;
import cz.hopik4kids.cms.scheduling.service.ScheduleService;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kindergarten morning programs and their invoicing ("Faktury školky", prd §6A.5).
 * <p>
 * The lesson is kept as a {@link Program} (type=school, schoolPart=morning): that is the single
 * source of truth the schedule/calendar, shifts and attendance already read, so entering a
 * kindergarten here makes it show up in the calendar without any duplicated schedule data.
 * The contract adds only the billing side (payer, price, price unit).
 */
@Service
public class SchoolContractService {

    private final SchoolContractRepository contracts;
    private final ProgramRepository programs;
    private final AdminProgramService programService;
    private final ScheduleService schedule;
    private final LessonOverrideRepository overrides;
    private final InvoiceService invoices;
    private final InvoiceRepository invoiceRepo;
    private final AuditService audit;

    public SchoolContractService(SchoolContractRepository contracts,
                                 ProgramRepository programs,
                                 AdminProgramService programService,
                                 ScheduleService schedule,
                                 LessonOverrideRepository overrides,
                                 InvoiceService invoices,
                                 InvoiceRepository invoiceRepo,
                                 AuditService audit) {
        this.contracts = contracts;
        this.programs = programs;
        this.programService = programService;
        this.schedule = schedule;
        this.overrides = overrides;
        this.invoices = invoices;
        this.invoiceRepo = invoiceRepo;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public List<SchoolContractDto> list() {
        return contracts.findAllByOrderByCreatedAtAsc().stream().map(this::dtoOf).toList();
    }

    @Transactional(readOnly = true)
    public SchoolContractDto get(String id) {
        return dtoOf(find(id));
    }

    /**
     * School programs that have no billing record yet — offered when adding a kindergarten so
     * already-entered programs get linked instead of duplicated.
     */
    @Transactional(readOnly = true)
    public List<AdminProgramDto> candidates() {
        Set<String> taken = new HashSet<>();
        for (SchoolContract c : contracts.findAll()) {
            taken.add(c.getProgramId());
        }
        return programs.findAll().stream()
                .filter(p -> p.getType() == ProgramType.SCHOOL)
                .filter(p -> p.getStatus() != cz.hopik4kids.cms.core.domain.ProgramStatus.ARCHIVED)
                .filter(p -> !taken.contains(p.getId()))
                .map(AdminProgramDto::from)
                .toList();
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public SchoolContractDto create(SchoolContractRequest req) {
        String programId;
        if (req.programId() != null && !req.programId().isBlank()) {
            // Link an existing school program and write the (optional) lesson edits through to it.
            Program existing = program(req.programId());
            if (contracts.findByProgramId(existing.getId()).isPresent()) {
                throw ApiException.conflict("CONTRACT_EXISTS", "Tento program už má fakturační údaje");
            }
            programService.update(existing.getId(), programRequest(AdminProgramDto.from(existing), req));
            programId = existing.getId();
        } else {
            if (req.schoolName() == null || req.schoolName().isBlank()) {
                throw ApiException.badRequest("MISSING_NAME", "Název školky je povinný");
            }
            programId = programService.create(newProgramRequest(req)).id();
        }

        SchoolContract c = new SchoolContract();
        c.setProgramId(programId);
        applyBilling(c, req);
        c = contracts.save(c);
        audit.record("create", "SchoolContract", c.getId());
        return dtoOf(c);
    }

    @Transactional
    public SchoolContractDto update(String id, SchoolContractRequest req) {
        SchoolContract c = find(id);
        Program p = program(c.getProgramId());
        programService.update(p.getId(), programRequest(AdminProgramDto.from(p), req));
        applyBilling(c, req);
        c = contracts.save(c);
        audit.record("update", "SchoolContract", c.getId());
        return dtoOf(c);
    }

    /**
     * Removes the billing record only — the program itself (and its calendar entries, attendance
     * and shifts) stays. Blocked once invoices exist: they must keep pointing at the contract
     * for the 10-year accounting trail.
     */
    @Transactional
    public void delete(String id) {
        SchoolContract c = find(id);
        int issued = invoiceRepo.findBySchoolContractIdOrderByIssueDateDesc(id).size();
        if (issued > 0) {
            throw ApiException.conflict("CONTRACT_HAS_INVOICES",
                    "Školku nelze odebrat — má vystavené faktury (" + issued + "). "
                            + "Ukonči jí období, nebo program archivuj v Programech.");
        }
        contracts.delete(c);
        audit.record("delete", "SchoolContract", id);
    }

    // --------------------------------------------------------------- invoice

    /** Issue an invoice to the kindergarten for a period of its morning program. */
    @Transactional
    public InvoiceDto issueInvoice(String id, SchoolInvoiceRequest req) {
        SchoolContract c = find(id);
        Program p = program(c.getProgramId());

        LocalDate from = req.periodFrom() != null ? req.periodFrom() : p.getValidFrom();
        LocalDate to = req.periodTo() != null ? req.periodTo() : p.getValidTo();
        if (from == null || to == null) {
            throw ApiException.badRequest("MISSING_PERIOD",
                    "Doplň období (u programu chybí platnost od/do)");
        }
        if (from.isAfter(to)) {
            throw ApiException.badRequest("INVALID_PERIOD", "Období 'od' nesmí být po 'do'");
        }
        if (c.getBillingEmail() == null || c.getBillingEmail().isBlank()) {
            // Not fatal — the invoice can be downloaded as PDF — but worth flagging early.
            audit.record("school-invoice-no-email", "SchoolContract", c.getId());
        }

        List<LocalDate> lessonDates = lessonDates(p, from, to);
        int lessons = req.lessonCount() != null ? Math.max(0, req.lessonCount()) : lessonDates.size();
        int months = monthsOf(lessonDates);

        String label = "Pohybový program " + p.getName()
                + " (" + czDate(from) + " – " + czDate(to) + ")";
        List<InvoiceService.Item> items = new ArrayList<>();
        int total;

        if (req.totalAmount() != null) {
            // Hand-entered amount wins over the computed one.
            total = req.totalAmount();
            items.add(new InvoiceService.Item(label, 1, total));
        } else {
            int qty = switch (c.getPriceUnit()) {
                case LESSON -> lessons;
                case MONTH -> months;
                case PERIOD -> 1;
            };
            if (qty <= 0) {
                throw ApiException.badRequest("NO_LESSONS",
                        "V zadaném období nejsou žádné lekce — zadej počet lekcí nebo částku ručně");
            }
            String unitLabel = switch (c.getPriceUnit()) {
                case LESSON -> label + " — " + qty + " lekcí";
                case MONTH -> label + " — " + qty + " měs.";
                case PERIOD -> label;
            };
            items.add(new InvoiceService.Item(unitLabel, qty, c.getPrice()));
            total = qty * c.getPrice();
        }

        String payerName = c.getBillingName() != null && !c.getBillingName().isBlank()
                ? c.getBillingName()
                : p.getName();
        String payerAddress = c.getBillingAddress() != null && !c.getBillingAddress().isBlank()
                ? c.getBillingAddress()
                : (p.getLocation() == null ? null : p.getLocation().getAddress());

        return invoices.createForSchoolContract(c, payerName, payerAddress, items, total, from, to);
    }

    /** Lessons actually taught in the range: recurring occurrences minus cancelled ones. */
    @Transactional(readOnly = true)
    public List<LocalDate> lessonDates(Program p, LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>(schedule.occurrenceDates(p, from, to));
        Set<LocalDate> cancelled = new HashSet<>();
        for (var o : overrides.findInRange(from, to)) {
            if (o.getType() == LessonOverrideType.CANCELLED
                    && p.getId().equals(o.getProgramId())
                    && o.getOriginalDate() != null) {
                cancelled.add(o.getOriginalDate());
            }
        }
        dates.removeAll(cancelled);
        return dates;
    }

    // --------------------------------------------------------------- helpers

    private static int monthsOf(List<LocalDate> lessonDates) {
        Set<YearMonth> months = new HashSet<>();
        for (LocalDate d : lessonDates) {
            months.add(YearMonth.from(d));
        }
        return months.size();
    }

    private void applyBilling(SchoolContract c, SchoolContractRequest req) {
        if (req.billingName() != null) {
            c.setBillingName(blankToNull(req.billingName()));
        }
        if (req.billingEmail() != null) {
            c.setBillingEmail(blankToNull(req.billingEmail()));
        }
        if (req.billingAddress() != null) {
            c.setBillingAddress(blankToNull(req.billingAddress()));
        }
        if (req.ico() != null) {
            c.setIco(blankToNull(req.ico()));
        }
        if (req.dic() != null) {
            c.setDic(blankToNull(req.dic()));
        }
        if (req.price() != null) {
            if (req.price() < 0) {
                throw ApiException.badRequest("INVALID_PRICE", "Cena nesmí být záporná");
            }
            c.setPrice(req.price());
        }
        if (req.priceUnit() != null && !req.priceUnit().isBlank()) {
            c.setPriceUnit(EnumParser.parse(SchoolPriceUnit.class, req.priceUnit(), "priceUnit"));
        }
        if (req.note() != null) {
            c.setNote(blankToNull(req.note()));
        }
        if (c.getBillingEmail() != null && !c.getBillingEmail().contains("@")) {
            throw ApiException.badRequest("INVALID_EMAIL", "Neplatný e-mail");
        }
    }

    /** New hidden school program for a kindergarten entered straight on the billing page. */
    private AdminProgramRequest newProgramRequest(SchoolContractRequest req) {
        return new AdminProgramRequest(
                "school",
                req.schoolName(),
                null,
                blankToNull(req.locationId()),
                0,                       // children don't pay for a morning program — the school does
                null,                    // no capacity (whole class takes part)
                "public",
                null,
                null,
                "none",
                "hidden",                // internal only: schedule/shifts yes, public website no
                req.weekday(),
                blankToNull(req.time()),
                "morning",
                req.periodFrom(),
                req.periodTo(),
                req.durationMin(),
                req.trainersNeeded(),
                null,
                null,
                null);
    }

    /**
     * Update payload that changes only the fields this page owns and preserves everything else
     * on the program (slug, capacity, access, trainers, …).
     */
    private AdminProgramRequest programRequest(AdminProgramDto cur, SchoolContractRequest req) {
        return new AdminProgramRequest(
                cur.type(),
                req.schoolName() != null && !req.schoolName().isBlank() ? req.schoolName() : cur.name(),
                cur.slug(),
                blankToNull(req.locationId()) != null ? req.locationId() : cur.locationId(),
                cur.price(),
                cur.capacity(),
                cur.accessMode(),
                cur.restrictionNote(),
                null,                    // never touches the access code
                cur.shirtPolicy(),
                cur.status(),
                req.weekday() != null ? req.weekday() : cur.weekday(),
                blankToNull(req.time()) != null ? req.time() : cur.time(),
                cur.schoolPart() != null ? cur.schoolPart() : "morning",
                req.periodFrom() != null ? req.periodFrom() : cur.validFrom(),
                req.periodTo() != null ? req.periodTo() : cur.validTo(),
                req.durationMin() != null ? req.durationMin() : cur.durationMin(),
                req.trainersNeeded() != null ? req.trainersNeeded() : cur.trainersNeeded(),
                cur.startDate(),
                cur.endDate(),
                cur.trainerIds());
    }

    private SchoolContractDto dtoOf(SchoolContract c) {
        Program p = programs.findByIdWithLocation(c.getProgramId())
                .orElseThrow(() -> ApiException.notFound("Program školky nenalezen"));
        List<LocalDate> dates = p.getValidFrom() != null && p.getValidTo() != null
                ? lessonDates(p, p.getValidFrom(), p.getValidTo())
                : List.of();
        int qty = switch (c.getPriceUnit()) {
            case LESSON -> dates.size();
            case MONTH -> monthsOf(dates);
            case PERIOD -> 1;
        };
        return new SchoolContractDto(
                c.getId(),
                p.getId(),
                p.getName(),
                p.getLocation() == null ? null : p.getLocation().getId(),
                p.getLocation() == null ? null : p.getLocation().getName(),
                p.getLocation() == null ? null : p.getLocation().getAddress(),
                p.getWeekday(),
                p.getTime(),
                p.getDurationMin(),
                p.getValidFrom(),
                p.getValidTo(),
                p.getTrainersNeeded(),
                p.getStatus().name().toLowerCase(),
                c.getBillingName(),
                c.getBillingEmail(),
                c.getBillingAddress(),
                c.getIco(),
                c.getDic(),
                c.getPrice(),
                c.getPriceUnit().name().toLowerCase(),
                c.getNote(),
                dates.size(),
                qty * c.getPrice());
    }

    private SchoolContract find(String id) {
        return contracts.findById(id)
                .orElseThrow(() -> ApiException.notFound("Školka nenalezena"));
    }

    private Program program(String programId) {
        return programs.findByIdWithLocation(programId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_PROGRAM", "Program nenalezen"));
    }

    private static final java.time.format.DateTimeFormatter CZ_DATE =
            java.time.format.DateTimeFormatter.ofPattern("d. M. yyyy");

    private static String czDate(LocalDate d) {
        return d == null ? "—" : d.format(CZ_DATE);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
