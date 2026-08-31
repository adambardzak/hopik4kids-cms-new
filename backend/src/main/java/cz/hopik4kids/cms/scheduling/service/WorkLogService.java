package cz.hopik4kids.cms.scheduling.service;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.SecurityUtils;
import cz.hopik4kids.cms.scheduling.domain.ShiftSignup;
import cz.hopik4kids.cms.scheduling.domain.ShiftStatus;
import cz.hopik4kids.cms.scheduling.domain.WorkLog;
import cz.hopik4kids.cms.scheduling.domain.WorkLogSource;
import cz.hopik4kids.cms.scheduling.domain.WorkLogStatus;
import cz.hopik4kids.cms.scheduling.repository.ShiftSignupRepository;
import cz.hopik4kids.cms.scheduling.repository.WorkLogRepository;
import cz.hopik4kids.cms.scheduling.web.dto.WorkLogDto;
import cz.hopik4kids.cms.scheduling.web.dto.WorkLogRequest;
import cz.hopik4kids.cms.scheduling.web.dto.WorkLogSummaryDto;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Timesheets for part-time staff (prd todo #3). Trainers record hours (PENDING), admins approve.
 * Entries can be seeded from approved shift signups (source = SHIFT), then edited/confirmed.
 */
@Service
public class WorkLogService {

    private final WorkLogRepository logs;
    private final ShiftSignupRepository signups;
    private final ProgramRepository programs;
    private final UserRepository users;
    private final AuditService audit;

    public WorkLogService(WorkLogRepository logs, ShiftSignupRepository signups,
                          ProgramRepository programs, UserRepository users, AuditService audit) {
        this.logs = logs;
        this.signups = signups;
        this.programs = programs;
        this.users = users;
        this.audit = audit;
    }

    // --- listing ---

    /** Trainer sees only their own entries; privileged roles see everyone's. */
    @Transactional(readOnly = true)
    public List<WorkLogDto> list(LocalDate from, LocalDate to) {
        List<WorkLog> rows;
        if (SecurityUtils.isPrivileged()) {
            rows = (from != null && to != null)
                    ? logs.findByWorkDateBetweenOrderByWorkDateDesc(from, to)
                    : logs.findAllByOrderByWorkDateDesc();
        } else {
            String me = SecurityUtils.currentUserId();
            rows = (from != null && to != null)
                    ? logs.findByTrainerIdAndWorkDateBetweenOrderByWorkDateDesc(me, from, to)
                    : logs.findByTrainerIdOrderByWorkDateDesc(me);
        }
        return rows.stream().map(this::toDto).toList();
    }

    /** Aggregate approved/pending hours per trainer for a period (admin overview + export). */
    @Transactional(readOnly = true)
    public List<WorkLogSummaryDto> summary(LocalDate from, LocalDate to) {
        List<WorkLog> rows = (from != null && to != null)
                ? logs.findByWorkDateBetweenOrderByWorkDateDesc(from, to)
                : logs.findAllByOrderByWorkDateDesc();

        Map<String, BigDecimal> approved = new HashMap<>();
        Map<String, BigDecimal> pending = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (WorkLog w : rows) {
            counts.merge(w.getTrainerId(), 1, Integer::sum);
            if (w.getStatus() == WorkLogStatus.APPROVED) {
                approved.merge(w.getTrainerId(), w.getHours(), BigDecimal::add);
            } else if (w.getStatus() == WorkLogStatus.PENDING) {
                pending.merge(w.getTrainerId(), w.getHours(), BigDecimal::add);
            }
        }
        List<WorkLogSummaryDto> out = new ArrayList<>();
        for (String trainerId : counts.keySet()) {
            out.add(new WorkLogSummaryDto(
                    trainerId,
                    users.findById(trainerId).map(User::getName).orElse("—"),
                    approved.getOrDefault(trainerId, BigDecimal.ZERO),
                    pending.getOrDefault(trainerId, BigDecimal.ZERO),
                    counts.get(trainerId)));
        }
        out.sort(Comparator.comparing(WorkLogSummaryDto::trainerName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    // --- trainer actions ---

    @Transactional
    public WorkLogDto create(WorkLogRequest req) {
        String me = SecurityUtils.currentUserId();
        WorkLog w = new WorkLog();
        w.setTrainerId(me);
        w.setWorkDate(req.workDate());
        w.setHours(req.hours());
        w.setNote(req.note());
        w.setProgramId(blankToNull(req.programId()));
        w.setSource(WorkLogSource.MANUAL);
        w.setStatus(WorkLogStatus.PENDING);
        w = logs.save(w);
        audit.record("worklog.create", "WorkLog", w.getId(), req.workDate() + " " + req.hours() + "h");
        return toDto(w);
    }

    @Transactional
    public WorkLogDto update(String id, WorkLogRequest req) {
        WorkLog w = mustAccess(id);
        // Once approved, only privileged users may edit.
        if (w.getStatus() == WorkLogStatus.APPROVED && !SecurityUtils.isPrivileged()) {
            throw ApiException.badRequest("ALREADY_APPROVED", "Schválený výkaz už nelze upravit");
        }
        w.setWorkDate(req.workDate());
        w.setHours(req.hours());
        w.setNote(req.note());
        w.setProgramId(blankToNull(req.programId()));
        // Editing a rejected/approved entry as a trainer sends it back to pending.
        if (!SecurityUtils.isPrivileged()) {
            w.setStatus(WorkLogStatus.PENDING);
        }
        logs.save(w);
        audit.record("worklog.update", "WorkLog", id);
        return toDto(w);
    }

    @Transactional
    public void delete(String id) {
        WorkLog w = mustAccess(id);
        if (w.getStatus() == WorkLogStatus.APPROVED && !SecurityUtils.isPrivileged()) {
            throw ApiException.badRequest("ALREADY_APPROVED", "Schválený výkaz nelze smazat");
        }
        logs.delete(w);
        audit.record("worklog.delete", "WorkLog", id);
    }

    // --- admin actions ---

    @Transactional
    public WorkLogDto setStatus(String id, String status) {
        WorkLog w = logs.findById(id)
                .orElseThrow(() -> ApiException.notFound("Výkaz nenalezen"));
        WorkLogStatus st;
        try {
            st = WorkLogStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_STATUS", "Neplatný stav");
        }
        w.setStatus(st);
        logs.save(w);
        audit.record("worklog.status", "WorkLog", id, st.name());
        return toDto(w);
    }

    /**
     * Seed PENDING work-log entries from a trainer's approved shift signups in a period
     * (prd todo #3 "kombinace"). Hours derived from the program's lesson duration. Idempotent:
     * skips (trainer, program, date) that already has a SHIFT-sourced row.
     */
    @Transactional
    public int seedFromShifts(LocalDate from, LocalDate to) {
        String me = SecurityUtils.currentUserId();
        int created = 0;
        for (ShiftSignup s : signups.findByTrainerIdAndLessonDateGreaterThanEqualOrderByLessonDateAsc(me, from)) {
            if (s.getLessonDate().isAfter(to) || s.getStatus() != ShiftStatus.APPROVED) {
                continue;
            }
            if (logs.findByTrainerIdAndProgramIdAndWorkDateAndSource(
                    me, s.getProgramId(), s.getLessonDate(), WorkLogSource.SHIFT).isPresent()) {
                continue;
            }
            Program p = programs.findById(s.getProgramId()).orElse(null);
            BigDecimal hours = hoursOf(p);
            WorkLog w = new WorkLog();
            w.setTrainerId(me);
            w.setWorkDate(s.getLessonDate());
            w.setHours(hours);
            w.setProgramId(s.getProgramId());
            w.setNote(p != null ? p.getName() : null);
            w.setSource(WorkLogSource.SHIFT);
            w.setStatus(WorkLogStatus.PENDING);
            logs.save(w);
            created++;
        }
        if (created > 0) {
            audit.record("worklog.seed", "WorkLog", me, created + " z směn");
        }
        return created;
    }

    // --- helpers ---
    private WorkLog mustAccess(String id) {
        WorkLog w = logs.findById(id)
                .orElseThrow(() -> ApiException.notFound("Výkaz nenalezen"));
        if (!SecurityUtils.isPrivileged() && !w.getTrainerId().equals(SecurityUtils.currentUserId())) {
            throw ApiException.forbidden("FORBIDDEN", "Nemáš přístup k tomuto výkazu");
        }
        return w;
    }

    private static BigDecimal hoursOf(Program p) {
        int minutes = (p != null && p.getDurationMin() != null && p.getDurationMin() > 0)
                ? p.getDurationMin() : 60;
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private WorkLogDto toDto(WorkLog w) {
        String trainerName = users.findById(w.getTrainerId()).map(User::getName).orElse("—");
        String programName = w.getProgramId() == null ? null
                : programs.findById(w.getProgramId()).map(Program::getName).orElse(null);
        return WorkLogDto.from(w, trainerName, programName);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // --- export (payroll) ---

    private static final String[] EXPORT_HEADERS = {
            "Datum", "Osoba", "Hodiny", "Program / poznámka", "Zdroj", "Stav"
    };

    @Transactional(readOnly = true)
    public byte[] exportCsv(LocalDate from, LocalDate to) {
        List<WorkLogDto> rows = list(from, to);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(String.join(";", EXPORT_HEADERS)).append('\n');
        for (WorkLogDto r : rows) {
            String[] c = exportCells(r);
            for (int i = 0; i < c.length; i++) {
                if (i > 0) sb.append(';');
                sb.append(csvEscape(c[i]));
            }
            sb.append('\n');
        }
        // Trailing per-trainer summary.
        sb.append('\n').append("Souhrn;Osoba;Schváleno;Čeká;Záznamů\n");
        for (WorkLogSummaryDto s : summary(from, to)) {
            sb.append(';').append(csvEscape(s.trainerName())).append(';')
                    .append(s.approvedHours()).append(';')
                    .append(s.pendingHours()).append(';')
                    .append(s.entryCount()).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportXlsx(LocalDate from, LocalDate to) {
        try (org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Výkazy");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
            }
            int r = 1;
            for (WorkLogDto row : list(from, to)) {
                org.apache.poi.ss.usermodel.Row xr = sheet.createRow(r++);
                String[] c = exportCells(row);
                for (int i = 0; i < c.length; i++) {
                    xr.createCell(i).setCellValue(c[i] == null ? "" : c[i]);
                }
            }
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            org.apache.poi.ss.usermodel.Sheet sum = wb.createSheet("Souhrn");
            org.apache.poi.ss.usermodel.Row sh = sum.createRow(0);
            String[] sumHeaders = {"Osoba", "Schváleno (h)", "Čeká (h)", "Záznamů"};
            for (int i = 0; i < sumHeaders.length; i++) {
                sh.createCell(i).setCellValue(sumHeaders[i]);
            }
            int sr = 1;
            for (WorkLogSummaryDto s : summary(from, to)) {
                org.apache.poi.ss.usermodel.Row xr = sum.createRow(sr++);
                xr.createCell(0).setCellValue(s.trainerName());
                xr.createCell(1).setCellValue(s.approvedHours().doubleValue());
                xr.createCell(2).setCellValue(s.pendingHours().doubleValue());
                xr.createCell(3).setCellValue(s.entryCount());
            }
            for (int i = 0; i < sumHeaders.length; i++) {
                sum.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "EXPORT_ERROR", "Export selhal");
        }
    }

    private static String[] exportCells(WorkLogDto r) {
        String label = r.programName() != null ? r.programName()
                : (r.note() != null ? r.note() : "");
        return new String[]{
                r.workDate().toString(),
                r.trainerName(),
                r.hours().toPlainString(),
                label,
                "shift".equals(r.source()) ? "ze směny" : "ručně",
                statusCs(r.status())
        };
    }

    private static String statusCs(String status) {
        return switch (status) {
            case "approved" -> "Schváleno";
            case "rejected" -> "Zamítnuto";
            default -> "Čeká";
        };
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
