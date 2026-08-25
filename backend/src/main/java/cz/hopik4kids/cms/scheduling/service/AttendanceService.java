package cz.hopik4kids.cms.scheduling.service;

import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.EnumParser;
import cz.hopik4kids.cms.kernel.web.SecurityUtils;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.scheduling.domain.AttendanceRecord;
import cz.hopik4kids.cms.scheduling.domain.AttendanceStatus;
import cz.hopik4kids.cms.scheduling.repository.AttendanceRecordRepository;
import cz.hopik4kids.cms.scheduling.web.dto.AttendanceRowDto;
import cz.hopik4kids.cms.scheduling.web.dto.AttendanceSaveRequest;
import cz.hopik4kids.cms.scheduling.web.dto.AttendanceStatsDto;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Attendance recording + statistics (prd §6A.3). */
@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendance;
    private final RegistrationRepository registrations;
    private final ProgramRepository programs;
    private final AuditService audit;

    public AttendanceService(AttendanceRecordRepository attendance,
                             RegistrationRepository registrations,
                             ProgramRepository programs,
                             AuditService audit) {
        this.attendance = attendance;
        this.registrations = registrations;
        this.programs = programs;
        this.audit = audit;
    }

    /** Trainers may only touch programs they are assigned to (prd §7.5). Owner/admin: full access. */
    private void requireProgramAccess(String programId) {
        if (SecurityUtils.isPrivileged()) {
            return;
        }
        String userId = SecurityUtils.currentUserId();
        if (userId == null || !programs.isTrainerAssigned(programId, userId)) {
            throw ApiException.forbidden("NOT_ASSIGNED", "Nemáš přístup k tomuto programu");
        }
    }

    /** Roster for a lesson: all active children of the program + their recorded status (if any). */
    @Transactional(readOnly = true)
    public List<AttendanceRowDto> roster(String programId, LocalDate date) {
        requireProgramAccess(programId);
        Map<String, AttendanceRecord> byChild = new java.util.HashMap<>();
        for (AttendanceRecord r : attendance.findByProgramIdAndLessonDate(programId, date)) {
            byChild.put(r.getChildId(), r);
        }

        List<AttendanceRowDto> rows = new ArrayList<>();
        for (Registration reg : registrations.findActiveWithChildByProgram(programId)) {
            var child = reg.getChild();
            AttendanceRecord rec = byChild.get(child.getId());
            rows.add(new AttendanceRowDto(
                    child.getId(),
                    child.getFullName(),
                    rec == null ? null : rec.getStatus().name().toLowerCase(),
                    rec == null ? null : rec.getNote()));
        }
        return rows;
    }

    @Transactional
    public void save(String programId, LocalDate date, AttendanceSaveRequest req) {
        requireProgramAccess(programId);
        if (req.entries() == null) {
            return;
        }
        var existing = attendance.findByProgramIdAndLessonDate(programId, date);
        Map<String, AttendanceRecord> byChild = new java.util.HashMap<>();
        for (AttendanceRecord r : existing) {
            byChild.put(r.getChildId(), r);
        }

        for (AttendanceSaveRequest.Entry e : req.entries()) {
            if (e.status() == null || e.status().isBlank()) {
                // No status -> remove any existing record (mark as not-recorded).
                AttendanceRecord rec = byChild.get(e.childId());
                if (rec != null) {
                    attendance.delete(rec);
                }
                continue;
            }
            AttendanceStatus status = EnumParser.parseRequired(AttendanceStatus.class, e.status(), "status");
            AttendanceRecord rec = byChild.get(e.childId());
            if (rec == null) {
                rec = new AttendanceRecord();
                rec.setProgramId(programId);
                rec.setChildId(e.childId());
                rec.setLessonDate(date);
            }
            rec.setStatus(status);
            rec.setNote(e.note() == null || e.note().isBlank() ? null : e.note());
            attendance.save(rec);
        }
        audit.record("attendance-save", "Program", programId, "{\"date\":\"" + date + "\"}");
    }

    @Transactional(readOnly = true)
    public AttendanceStatsDto stats(String programId) {
        requireProgramAccess(programId);
        List<AttendanceRecord> all = attendance.findByProgramId(programId);

        // per-child totals
        Map<String, int[]> childCounts = new java.util.HashMap<>(); // [present, excused, absent]
        for (AttendanceRecord r : all) {
            int[] c = childCounts.computeIfAbsent(r.getChildId(), k -> new int[3]);
            switch (r.getStatus()) {
                case PRESENT -> c[0]++;
                case EXCUSED -> c[1]++;
                case ABSENT -> c[2]++;
            }
        }

        // resolve child names from active registrations
        Map<String, String> names = new java.util.HashMap<>();
        for (Registration reg : registrations.findActiveWithChildByProgram(programId)) {
            names.put(reg.getChild().getId(), reg.getChild().getFullName());
        }

        List<AttendanceStatsDto.PerChild> perChild = new ArrayList<>();
        childCounts.forEach((childId, c) -> perChild.add(new AttendanceStatsDto.PerChild(
                childId,
                names.getOrDefault(childId, "—"),
                c[0], c[1], c[2], c[0] + c[1] + c[2])));
        perChild.sort(Comparator.comparing(AttendanceStatsDto.PerChild::childName));

        // per-lesson totals (grouped by date)
        Map<LocalDate, int[]> lessonCounts = new java.util.TreeMap<>();
        for (AttendanceRecord r : all) {
            int[] c = lessonCounts.computeIfAbsent(r.getLessonDate(), k -> new int[3]);
            switch (r.getStatus()) {
                case PRESENT -> c[0]++;
                case EXCUSED -> c[1]++;
                case ABSENT -> c[2]++;
            }
        }
        List<AttendanceStatsDto.PerLesson> perLesson = new ArrayList<>();
        lessonCounts.forEach((date, c) -> perLesson.add(
                new AttendanceStatsDto.PerLesson(date.toString(), c[0], c[1], c[2])));

        return new AttendanceStatsDto(perChild, perLesson);
    }
}
