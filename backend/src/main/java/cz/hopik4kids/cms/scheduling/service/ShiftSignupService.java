package cz.hopik4kids.cms.scheduling.service;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramStatus;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.SecurityUtils;
import cz.hopik4kids.cms.scheduling.domain.ShiftSignup;
import cz.hopik4kids.cms.scheduling.domain.ShiftStatus;
import cz.hopik4kids.cms.scheduling.repository.ShiftSignupRepository;
import cz.hopik4kids.cms.scheduling.web.dto.ShiftSlotDto;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shift-signup (prd §7.4): trainers sign up for lesson occurrences (program + date).
 * Signups start as PENDING and are approved/rejected by an admin. Occurrences are expanded
 * from recurring programs via {@link ScheduleService} (no LessonInstance rows).
 */
@Service
public class ShiftSignupService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<ShiftStatus> ACTIVE = List.of(ShiftStatus.PENDING, ShiftStatus.APPROVED);

    private final ShiftSignupRepository signups;
    private final ProgramRepository programs;
    private final UserRepository users;
    private final ScheduleService schedule;
    private final AuditService audit;

    public ShiftSignupService(ShiftSignupRepository signups, ProgramRepository programs,
                              UserRepository users, ScheduleService schedule, AuditService audit) {
        this.signups = signups;
        this.programs = programs;
        this.users = users;
        this.schedule = schedule;
        this.audit = audit;
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw ApiException.badRequest("INVALID_RANGE", "Neplatné datumové rozmezí");
        }
        if (from.plusDays(184).isBefore(to)) {
            throw ApiException.badRequest("RANGE_TOO_LARGE", "Rozsah je příliš velký (max půl roku)");
        }
    }

    /** All lesson occurrences in the range with occupancy + the current user's own signup status. */
    @Transactional(readOnly = true)
    public List<ShiftSlotDto> openSlots(LocalDate from, LocalDate to) {
        validateRange(from, to);
        String me = SecurityUtils.currentUserId();

        // Index signups by program+date for the range.
        Map<String, List<ShiftSignup>> byKey = new HashMap<>();
        for (ShiftSignup s : signups.findInRange(from, to)) {
            byKey.computeIfAbsent(key(s.getProgramId(), s.getLessonDate()), k -> new ArrayList<>()).add(s);
        }
        Map<String, String> trainerNames = new HashMap<>();

        List<ShiftSlotDto> slots = new ArrayList<>();
        for (Program p : programs.findByStatusWithLocation(ProgramStatus.ACTIVE)) {
            LocalTime start = parseTime(p.getTime());
            String startStr = start == null ? null : start.format(HHMM);
            String endStr = (start != null && p.getDurationMin() != null)
                    ? start.plusMinutes(p.getDurationMin()).format(HHMM) : null;

            for (LocalDate date : schedule.occurrenceDates(p, from, to)) {
                List<ShiftSignup> here = byKey.getOrDefault(key(p.getId(), date), List.of());
                int approved = 0, pending = 0;
                String mySignupId = null, myStatus = null;
                List<ShiftSlotDto.ShiftSignupTrainerDto> people = new ArrayList<>();
                for (ShiftSignup s : here) {
                    if (s.getStatus() == ShiftStatus.APPROVED) approved++;
                    else if (s.getStatus() == ShiftStatus.PENDING) pending++;
                    if (s.getTrainerId().equals(me) && s.getStatus() != ShiftStatus.CANCELLED
                            && s.getStatus() != ShiftStatus.REJECTED) {
                        mySignupId = s.getId();
                        myStatus = s.getStatus().name();
                    }
                    if (s.getStatus() == ShiftStatus.APPROVED || s.getStatus() == ShiftStatus.PENDING) {
                        String name = trainerNames.computeIfAbsent(s.getTrainerId(),
                                id -> users.findById(id).map(User::getName).orElse("—"));
                        people.add(new ShiftSlotDto.ShiftSignupTrainerDto(
                                s.getId(), s.getTrainerId(), name, s.getStatus().name()));
                    }
                }
                slots.add(new ShiftSlotDto(
                        p.getId(), p.getName(), p.getType().name().toLowerCase(), date,
                        startStr, endStr,
                        p.getLocation() == null ? null : p.getLocation().getName(),
                        p.getTrainersNeeded(), approved, pending, mySignupId, myStatus, people));
            }
        }
        slots.sort(Comparator.comparing(ShiftSlotDto::date)
                .thenComparing(s -> s.startTime() == null ? "" : s.startTime()));
        return slots;
    }

    /** Only slots the current trainer is signed up for (PENDING or APPROVED). */
    @Transactional(readOnly = true)
    public List<ShiftSlotDto> mySlots(LocalDate from, LocalDate to) {
        return openSlots(from, to).stream().filter(s -> s.myStatus() != null).toList();
    }

    @Transactional
    public void signup(String programId, LocalDate date) {
        String me = SecurityUtils.currentUserId();
        Program p = programs.findById(programId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_PROGRAM", "Program nenalezen"));
        if (p.getStatus() != ProgramStatus.ACTIVE) {
            throw ApiException.badRequest("PROGRAM_INACTIVE", "Program není aktivní");
        }
        // The (program, date) must be a real occurrence of this program.
        if (schedule.occurrenceDates(p, date, date).isEmpty()) {
            throw ApiException.badRequest("INVALID_OCCURRENCE", "V tento den lekce neprobíhá");
        }
        signups.findByProgramIdAndLessonDateAndTrainerId(programId, date, me).ifPresent(existing -> {
            if (existing.getStatus() == ShiftStatus.PENDING || existing.getStatus() == ShiftStatus.APPROVED) {
                throw ApiException.badRequest("ALREADY_SIGNED", "Na tuto hodinu už jsi přihlášený/á");
            }
            // Re-activate a previously cancelled/rejected signup.
            existing.setStatus(ShiftStatus.PENDING);
            signups.save(existing);
        });
        if (signups.findByProgramIdAndLessonDateAndTrainerId(programId, date, me)
                .filter(s -> s.getStatus() == ShiftStatus.PENDING).isEmpty()) {
            ShiftSignup s = new ShiftSignup();
            s.setProgramId(programId);
            s.setLessonDate(date);
            s.setTrainerId(me);
            s.setStatus(ShiftStatus.PENDING);
            signups.save(s);
        }
        ShiftSignup saved = signups.findByProgramIdAndLessonDateAndTrainerId(programId, date, me).orElseThrow();
        audit.record("shift.signup", "ShiftSignup", saved.getId(), programId + "@" + date);
    }

    @Transactional
    public void cancel(String signupId) {
        ShiftSignup s = signups.findById(signupId)
                .orElseThrow(() -> ApiException.notFound("Přihlášení nenalezeno"));
        if (!SecurityUtils.isPrivileged() && !s.getTrainerId().equals(SecurityUtils.currentUserId())) {
            throw ApiException.forbidden("FORBIDDEN", "Nemůžeš zrušit cizí přihlášení");
        }
        s.setStatus(ShiftStatus.CANCELLED);
        signups.save(s);
        audit.record("shift.cancel", "ShiftSignup", signupId);
    }

    // --- admin approval ---

    @Transactional(readOnly = true)
    public List<ShiftSlotDto.ShiftSignupTrainerDto> pending() {
        return signups.findAll().stream()
                .filter(s -> s.getStatus() == ShiftStatus.PENDING)
                .map(s -> new ShiftSlotDto.ShiftSignupTrainerDto(
                        s.getId(), s.getTrainerId(),
                        users.findById(s.getTrainerId()).map(User::getName).orElse("—"),
                        s.getStatus().name()))
                .toList();
    }

    @Transactional
    public void decide(String signupId, boolean approve) {
        ShiftSignup s = signups.findById(signupId)
                .orElseThrow(() -> ApiException.notFound("Přihlášení nenalezeno"));
        if (approve) {
            long approved = signups.countByProgramIdAndLessonDateAndStatusIn(
                    s.getProgramId(), s.getLessonDate(), List.of(ShiftStatus.APPROVED));
            Program p = programs.findById(s.getProgramId()).orElse(null);
            int needed = p == null ? 1 : p.getTrainersNeeded();
            if (approved >= needed) {
                throw ApiException.badRequest("SLOT_FULL", "Hodina je již plně obsazená");
            }
            s.setStatus(ShiftStatus.APPROVED);
        } else {
            s.setStatus(ShiftStatus.REJECTED);
        }
        signups.save(s);
        audit.record(approve ? "shift.approve" : "shift.reject", "ShiftSignup", signupId);
    }

    private static String key(String programId, LocalDate date) {
        return programId + "|" + date;
    }

    private LocalTime parseTime(String hhmm) {
        if (hhmm == null || hhmm.isBlank()) return null;
        try {
            return LocalTime.parse(hhmm.trim(), HHMM);
        } catch (Exception e) {
            return null;
        }
    }
}
