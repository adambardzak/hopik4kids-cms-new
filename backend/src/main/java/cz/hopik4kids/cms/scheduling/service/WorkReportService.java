package cz.hopik4kids.cms.scheduling.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.pdf.PdfFonts;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.SecurityUtils;
import cz.hopik4kids.cms.scheduling.domain.WorkLog;
import cz.hopik4kids.cms.scheduling.domain.WorkLogStatus;
import cz.hopik4kids.cms.scheduling.domain.WorkReport;
import cz.hopik4kids.cms.scheduling.domain.WorkReportStatus;
import cz.hopik4kids.cms.scheduling.repository.WorkLogRepository;
import cz.hopik4kids.cms.scheduling.repository.WorkReportRepository;
import cz.hopik4kids.cms.scheduling.web.dto.WorkReportDto;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Monthly work reports: each person's month is assembled automatically from their work-log
 * entries (confirmed shifts create them), the person corrects and submits it, the owner approves
 * (or returns it), and the accountant downloads all approved reports of a month at once.
 */
@Service
public class WorkReportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d.M.yyyy");
    private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");
    /** The current month can be submitted from this many days before its last day. */
    private static final int SUBMIT_DAYS_BEFORE_END = 2;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Prague"));
    private static final String[] MONTHS_CS = {"leden", "únor", "březen", "duben", "květen", "červen",
            "červenec", "srpen", "září", "říjen", "listopad", "prosinec"};

    private final WorkReportRepository reports;
    private final WorkLogRepository logs;
    private final UserRepository users;
    private final ProgramRepository programs;
    private final AuditService audit;

    public WorkReportService(WorkReportRepository reports, WorkLogRepository logs, UserRepository users,
                             ProgramRepository programs, AuditService audit) {
        this.reports = reports;
        this.logs = logs;
        this.users = users;
        this.programs = programs;
        this.audit = audit;
    }

    // --- person's own report ---

    @Transactional(readOnly = true)
    public WorkReportDto mine(String period) {
        YearMonth ym = parse(period);
        String me = SecurityUtils.currentUserId();
        return toDto(user(me), ym, reports.findByUserIdAndPeriod(me, ym.toString()).orElse(null));
    }

    /** Lock the month and send it to the owner for approval. */
    @Transactional
    public WorkReportDto submit(String period) {
        YearMonth ym = parse(period);
        // Submitting locks the month, so the current month only opens in its last days.
        LocalDate today = LocalDate.now(PRAGUE);
        if (!ym.isBefore(YearMonth.from(today)) && today.isBefore(ym.atEndOfMonth().minusDays(SUBMIT_DAYS_BEFORE_END))) {
            throw ApiException.badRequest("MONTH_NOT_OVER",
                    "Výkaz za tento měsíc půjde odeslat až od " + ym.atEndOfMonth().minusDays(SUBMIT_DAYS_BEFORE_END).format(DATE));
        }
        String me = SecurityUtils.currentUserId();
        User u = user(me);
        WorkReport r = reports.findByUserIdAndPeriod(me, ym.toString()).orElseGet(() -> {
            WorkReport n = new WorkReport();
            n.setUserId(me);
            n.setPeriod(ym.toString());
            return n;
        });
        if (r.getStatus() == WorkReportStatus.SUBMITTED || r.getStatus() == WorkReportStatus.APPROVED) {
            throw ApiException.badRequest("ALREADY_SUBMITTED", "Výkaz za tento měsíc už je odeslaný");
        }
        List<WorkLog> entries = billable(me, ym);
        if (entries.isEmpty()) {
            throw ApiException.badRequest("EMPTY_REPORT", "Ve výkazu nejsou žádné odpracované hodiny");
        }
        freeze(r, entries, u.getHourlyRate());
        r.setStatus(WorkReportStatus.SUBMITTED);
        r.setSubmittedAt(Instant.now());
        r.setReturnNote(null);
        reports.save(r);
        audit.record("workreport.submit", "WorkReport", r.getId(), ym.toString());
        return toDto(u, ym, r);
    }

    // --- owner / accountant overview ---

    /** Everyone who worked in the month (or has a report for it), with live/frozen totals. */
    @Transactional(readOnly = true)
    public List<WorkReportDto> overview(String period) {
        YearMonth ym = parse(period);
        Map<String, WorkReport> byUser = reports.findByPeriod(ym.toString()).stream()
                .collect(Collectors.toMap(WorkReport::getUserId, r -> r));
        Set<String> userIds = new LinkedHashSet<>(byUser.keySet());
        for (WorkLog w : logs.findByWorkDateBetweenOrderByWorkDateDesc(ym.atDay(1), ym.atEndOfMonth())) {
            userIds.add(w.getTrainerId());
        }
        Collator cs = Collator.getInstance(Locale.forLanguageTag("cs"));
        List<WorkReportDto> out = new ArrayList<>();
        for (String id : userIds) {
            users.findById(id).ifPresent(u -> out.add(toDto(u, ym, byUser.get(id))));
        }
        out.sort(Comparator.comparing(WorkReportDto::userName, cs));
        return out;
    }

    /** Owner approves: entries become approved, hours × rate are frozen. */
    @Transactional
    public WorkReportDto approve(String userId, String period) {
        YearMonth ym = parse(period);
        WorkReport r = reports.findByUserIdAndPeriod(userId, ym.toString())
                .orElseThrow(() -> ApiException.badRequest("NOT_SUBMITTED", "Výkaz ještě nebyl odeslán"));
        if (r.getStatus() != WorkReportStatus.SUBMITTED) {
            throw ApiException.badRequest("NOT_SUBMITTED", "Schválit lze jen odeslaný výkaz");
        }
        User u = user(userId);
        List<WorkLog> entries = billable(userId, ym);
        for (WorkLog w : entries) {
            if (w.getStatus() != WorkLogStatus.APPROVED) {
                w.setStatus(WorkLogStatus.APPROVED);
                logs.save(w);
            }
        }
        freeze(r, entries, u.getHourlyRate());
        r.setStatus(WorkReportStatus.APPROVED);
        r.setDecidedAt(Instant.now());
        r.setDecidedBy(SecurityUtils.currentUserId());
        reports.save(r);
        audit.record("workreport.approve", "WorkReport", r.getId(), ym + " " + r.getTotalHours() + "h");
        return toDto(u, ym, r);
    }

    /** Send back for corrections (also re-opens an approved month). */
    @Transactional
    public WorkReportDto returnForFix(String userId, String period, String note) {
        YearMonth ym = parse(period);
        WorkReport r = reports.findByUserIdAndPeriod(userId, ym.toString())
                .orElseThrow(() -> ApiException.badRequest("NOT_SUBMITTED", "Výkaz ještě nebyl odeslán"));
        if (r.getStatus() == WorkReportStatus.RETURNED) {
            throw ApiException.badRequest("ALREADY_RETURNED", "Výkaz už je vrácený k opravě");
        }
        r.setStatus(WorkReportStatus.RETURNED);
        r.setReturnNote(note == null || note.isBlank() ? null : note.trim());
        r.setDecidedAt(Instant.now());
        r.setDecidedBy(SecurityUtils.currentUserId());
        reports.save(r);
        audit.record("workreport.return", "WorkReport", r.getId(), ym.toString());
        return toDto(user(userId), ym, r);
    }

    // --- locking (used by WorkLogService) ---

    /** Status of the person's report for the month containing {@code date}, if any. */
    @Transactional(readOnly = true)
    public Optional<WorkReportStatus> statusFor(String userId, LocalDate date) {
        return reports.findByUserIdAndPeriod(userId, YearMonth.from(date).toString()).map(WorkReport::getStatus);
    }

    // --- documents ---

    @Transactional(readOnly = true)
    public byte[] pdf(String userId, String period) {
        YearMonth ym = parse(period);
        boolean staff = SecurityUtils.isPrivileged() || SecurityUtils.hasRole("ACCOUNTANT");
        if (!staff && !userId.equals(SecurityUtils.currentUserId())) {
            throw ApiException.forbidden("FORBIDDEN", "Nemáš přístup k tomuto výkazu");
        }
        // The accountant only works with approved reports.
        WorkReport r = reports.findByUserIdAndPeriod(userId, ym.toString()).orElse(null);
        if (SecurityUtils.hasRole("ACCOUNTANT") && (r == null || r.getStatus() != WorkReportStatus.APPROVED)) {
            throw ApiException.forbidden("NOT_APPROVED", "Výkaz ještě není schválený");
        }
        return renderPdf(user(userId), ym, r);
    }

    /** ZIP with a PDF per approved report of the month + a CSV summary (for payroll). */
    @Transactional(readOnly = true)
    public byte[] approvedZip(String period) {
        YearMonth ym = parse(period);
        List<WorkReport> approved = reports.findByPeriod(ym.toString()).stream()
                .filter(r -> r.getStatus() == WorkReportStatus.APPROVED)
                .toList();
        if (approved.isEmpty()) {
            throw ApiException.badRequest("NO_APPROVED", "Za tento měsíc zatím nejsou žádné schválené výkazy");
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            StringBuilder csv = new StringBuilder("﻿Osoba;Hodiny;Sazba (Kč/h);Částka (Kč);Schváleno\n");
            Set<String> names = new LinkedHashSet<>();
            for (WorkReport r : approved) {
                User u = user(r.getUserId());
                String base = slug(u.getName());
                String name = base;
                for (int i = 2; !names.add(name); i++) {
                    name = base + "-" + i;
                }
                zip.putNextEntry(new ZipEntry("vykaz-" + ym + "-" + name + ".pdf"));
                zip.write(renderPdf(u, ym, r));
                zip.closeEntry();
                csv.append(u.getName()).append(';')
                        .append(r.getTotalHours().toPlainString()).append(';')
                        .append(r.getHourlyRate() == null ? "" : r.getHourlyRate().toPlainString()).append(';')
                        .append(r.getTotalAmount() == null ? "" : r.getTotalAmount().toPlainString()).append(';')
                        .append(r.getDecidedAt() == null ? "" : DATE_TIME.format(r.getDecidedAt())).append('\n');
            }
            zip.putNextEntry(new ZipEntry("souhrn-" + ym + ".csv"));
            zip.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
            audit.record("workreport.export", "WorkReport", "-", ym + " " + approved.size() + "×");
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "EXPORT_ERROR", "Export selhal");
        }
    }

    // --- helpers ---

    /** Entries that count towards the report (rejected ones don't). */
    private List<WorkLog> billable(String userId, YearMonth ym) {
        return logs.findByTrainerIdAndWorkDateBetweenOrderByWorkDateDesc(userId, ym.atDay(1), ym.atEndOfMonth())
                .stream()
                .filter(w -> w.getStatus() != WorkLogStatus.REJECTED)
                .sorted(Comparator.comparing(WorkLog::getWorkDate))
                .toList();
    }

    private static void freeze(WorkReport r, List<WorkLog> entries, BigDecimal rate) {
        BigDecimal hours = sumHours(entries);
        r.setTotalHours(hours);
        r.setHourlyRate(rate);
        r.setTotalAmount(amount(hours, rate));
    }

    private static BigDecimal sumHours(List<WorkLog> entries) {
        return entries.stream().map(WorkLog::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal amount(BigDecimal hours, BigDecimal rate) {
        return rate == null ? null : hours.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private WorkReportDto toDto(User u, YearMonth ym, WorkReport r) {
        List<WorkLog> entries = billable(u.getId(), ym);
        int pending = (int) entries.stream().filter(w -> w.getStatus() == WorkLogStatus.PENDING).count();
        boolean frozen = r != null && r.getStatus() == WorkReportStatus.APPROVED;
        BigDecimal hours = frozen ? r.getTotalHours() : sumHours(entries);
        BigDecimal rate = frozen ? r.getHourlyRate() : u.getHourlyRate();
        BigDecimal total = frozen ? r.getTotalAmount() : amount(hours, rate);
        String decidedBy = r == null || r.getDecidedBy() == null ? null
                : users.findById(r.getDecidedBy()).map(User::getName).orElse(null);
        return new WorkReportDto(
                u.getId(), u.getName(), u.getRole().name().toLowerCase(), ym.toString(),
                r == null ? "draft" : r.getStatus().name().toLowerCase(),
                hours, rate, total, entries.size(), pending,
                r == null ? null : r.getSubmittedAt(),
                r == null ? null : r.getDecidedAt(),
                decidedBy,
                r == null ? null : r.getReturnNote());
    }

    private byte[] renderPdf(User u, YearMonth ym, WorkReport r) {
        WorkReportDto dto = toDto(u, ym, r);
        List<WorkLog> entries = billable(u.getId(), ym);
        Map<String, String> programNames = new java.util.HashMap<>();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Výkaz práce — " + monthLabel(ym), PdfFonts.bold(16)));
            Font meta = PdfFonts.regular(10, Color.DARK_GRAY);
            doc.add(new Paragraph(u.getName() + "  ·  " + u.getEmail(), meta));
            doc.add(new Paragraph("Stav: " + statusCs(dto.status())
                    + (dto.decidedAt() != null && "approved".equals(dto.status())
                    ? "  ·  schválil(a) " + nz(dto.decidedByName()) + " " + DATE_TIME.format(dto.decidedAt()) : ""),
                    meta));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{2f, 6f, 1.6f});
            table.setWidthPercentage(100);
            Font head = PdfFonts.bold(9, Color.WHITE);
            for (String h : new String[]{"Datum", "Činnost / poznámka", "Hodiny"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, head));
                c.setBackgroundColor(new Color(37, 99, 235));
                c.setPadding(5);
                table.addCell(c);
            }
            Font cell = PdfFonts.regular(9);
            boolean alt = false;
            for (WorkLog w : entries) {
                Color bg = alt ? new Color(245, 247, 250) : Color.WHITE;
                alt = !alt;
                addCell(table, w.getWorkDate().format(DATE), cell, bg, Element.ALIGN_LEFT);
                addCell(table, label(w, programNames), cell, bg, Element.ALIGN_LEFT);
                addCell(table, w.getHours().stripTrailingZeros().toPlainString(), cell, bg, Element.ALIGN_RIGHT);
            }
            doc.add(table);
            doc.add(new Paragraph(" "));

            Font sum = PdfFonts.bold(11);
            doc.add(new Paragraph("Odpracováno celkem: " + dto.totalHours().stripTrailingZeros().toPlainString() + " h", sum));
            if (dto.hourlyRate() != null) {
                doc.add(new Paragraph("Sazba: " + money(dto.hourlyRate()) + " Kč/h", PdfFonts.regular(10)));
                doc.add(new Paragraph("Odměna celkem: " + money(dto.totalAmount()) + " Kč", sum));
            } else {
                doc.add(new Paragraph("Sazba není nastavená.", PdfFonts.regular(10, Color.DARK_GRAY)));
            }
            if (dto.submittedAt() != null) {
                doc.add(new Paragraph("Odesláno: " + DATE_TIME.format(dto.submittedAt()), meta));
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_ERROR", "Nepodařilo se vytvořit PDF");
        }
    }

    private String label(WorkLog w, Map<String, String> programNames) {
        String program = w.getProgramId() == null ? null
                : programNames.computeIfAbsent(w.getProgramId(),
                        id -> programs.findById(id).map(Program::getName).orElse(null));
        if (program != null && w.getNote() != null && !w.getNote().equals(program)) {
            return program + " — " + w.getNote();
        }
        return program != null ? program : nz(w.getNote());
    }

    private static void addCell(PdfPTable t, String text, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private User user(String id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Uživatel nenalezen"));
    }

    private static YearMonth parse(String period) {
        try {
            return YearMonth.parse(period == null ? "" : period.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("INVALID_PERIOD", "Neplatný měsíc (očekáváno RRRR-MM)");
        }
    }

    private static String monthLabel(YearMonth ym) {
        return MONTHS_CS[ym.getMonthValue() - 1] + " " + ym.getYear();
    }

    private static String statusCs(String status) {
        return switch (status) {
            case "submitted" -> "odesláno ke schválení";
            case "approved" -> "schváleno";
            case "returned" -> "vráceno k opravě";
            default -> "rozpracováno";
        };
    }

    private static String money(BigDecimal v) {
        return v == null ? "" : String.format(Locale.forLanguageTag("cs"), "%,.2f", v);
    }

    private static String slug(String s) {
        String n = java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return n.isEmpty() ? "osoba" : n;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
