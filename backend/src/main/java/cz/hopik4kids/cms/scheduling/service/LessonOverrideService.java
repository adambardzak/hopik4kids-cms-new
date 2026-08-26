package cz.hopik4kids.cms.scheduling.service;

import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.scheduling.domain.LessonOverride;
import cz.hopik4kids.cms.scheduling.domain.LessonOverrideType;
import cz.hopik4kids.cms.scheduling.repository.LessonOverrideRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** CRUD for schedule overrides: cancel/move a recurring lesson, add a one-off (prd §7.4). */
@Service
public class LessonOverrideService {

    private final LessonOverrideRepository overrides;
    private final ProgramRepository programs;
    private final AuditService audit;

    public LessonOverrideService(LessonOverrideRepository overrides, ProgramRepository programs, AuditService audit) {
        this.overrides = overrides;
        this.programs = programs;
        this.audit = audit;
    }

    /** Cancel a single recurring occurrence (holiday/closure). */
    @Transactional
    public String cancel(String programId, LocalDate originalDate, String note) {
        requireProgram(programId);
        if (originalDate == null) {
            throw ApiException.badRequest("MISSING_DATE", "Chybí datum termínu");
        }
        LessonOverride o = new LessonOverride();
        o.setType(LessonOverrideType.CANCELLED);
        o.setProgramId(programId);
        o.setOriginalDate(originalDate);
        o.setNote(note);
        o = overrides.save(o);
        audit.record("shift.cancelLesson", "LessonOverride", o.getId());
        return o.getId();
    }

    /** Move a recurring occurrence to a new date/time (and optionally venue). */
    @Transactional
    public String move(String programId, LocalDate originalDate, LocalDate newDate, String newTime,
                       Integer durationMin, String locationId, String note) {
        requireProgram(programId);
        if (originalDate == null || newDate == null || newTime == null || newTime.isBlank()) {
            throw ApiException.badRequest("MISSING_FIELDS", "Vyplň původní i nový termín a čas");
        }
        LessonOverride o = new LessonOverride();
        o.setType(LessonOverrideType.MOVED);
        o.setProgramId(programId);
        o.setOriginalDate(originalDate);
        o.setDate(newDate);
        o.setTime(newTime);
        o.setDurationMin(durationMin);
        o.setLocationId(locationId);
        o.setNote(note);
        o = overrides.save(o);
        audit.record("shift.moveLesson", "LessonOverride", o.getId());
        return o.getId();
    }

    /** Add a one-off lesson/event (optionally tied to a program as a substitute). */
    @Transactional
    public String oneOff(String programId, String title, LocalDate date, String time,
                         Integer durationMin, String locationId, String note) {
        if (date == null || time == null || time.isBlank()) {
            throw ApiException.badRequest("MISSING_FIELDS", "Vyplň datum a čas");
        }
        if ((programId == null || programId.isBlank()) && (title == null || title.isBlank())) {
            throw ApiException.badRequest("MISSING_TITLE", "Vyplň program nebo název akce");
        }
        if (programId != null && !programId.isBlank()) {
            requireProgram(programId);
        }
        LessonOverride o = new LessonOverride();
        o.setType(LessonOverrideType.ONE_OFF);
        o.setProgramId(programId != null && !programId.isBlank() ? programId : null);
        o.setTitle(title);
        o.setDate(date);
        o.setTime(time);
        o.setDurationMin(durationMin);
        o.setLocationId(locationId);
        o.setNote(note);
        o = overrides.save(o);
        audit.record("shift.oneOff", "LessonOverride", o.getId());
        return o.getId();
    }

    /** Remove an override (e.g. un-cancel a lesson, delete a one-off). */
    @Transactional
    public void delete(String id) {
        LessonOverride o = overrides.findById(id)
                .orElseThrow(() -> ApiException.notFound("Úprava termínu nenalezena"));
        overrides.delete(o);
        audit.record("shift.deleteOverride", "LessonOverride", id);
    }

    private void requireProgram(String programId) {
        if (programId == null || !programs.existsById(programId)) {
            throw ApiException.badRequest("INVALID_PROGRAM", "Program nenalezen");
        }
    }
}
