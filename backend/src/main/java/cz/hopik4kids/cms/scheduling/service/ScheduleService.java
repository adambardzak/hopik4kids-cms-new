package cz.hopik4kids.cms.scheduling.service;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramStatus;
import cz.hopik4kids.cms.core.domain.ProgramType;
import cz.hopik4kids.cms.core.domain.Location;
import cz.hopik4kids.cms.core.repository.LocationRepository;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.SecurityUtils;
import cz.hopik4kids.cms.scheduling.domain.LessonOverride;
import cz.hopik4kids.cms.scheduling.domain.LessonOverrideType;
import cz.hopik4kids.cms.scheduling.repository.LessonOverrideRepository;
import cz.hopik4kids.cms.scheduling.web.dto.ScheduleEntryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the weekly schedule (prd §6A.8 A) by expanding recurring club/school programs into
 * concrete dated occurrences for a date range, then applying overrides (cancel/move/one-off, prd §7.4).
 */
@Service
public class ScheduleService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final ProgramRepository programs;
    private final LessonOverrideRepository overrides;
    private final LocationRepository locations;

    public ScheduleService(ProgramRepository programs, LessonOverrideRepository overrides,
                           LocationRepository locations) {
        this.programs = programs;
        this.overrides = overrides;
        this.locations = locations;
    }

    @Transactional(readOnly = true)
    public List<ScheduleEntryDto> forRange(LocalDate from, LocalDate to, String locationId) {
        // Cap the range to avoid unbounded expansion (prd §11 performance).
        if (from == null || to == null || from.isAfter(to)) {
            throw ApiException.badRequest("INVALID_RANGE", "Neplatné datumové rozmezí");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw ApiException.badRequest("RANGE_TOO_LARGE", "Rozsah je příliš velký (max 1 rok)");
        }

        List<ScheduleEntryDto> entries = new ArrayList<>();

        // Load overrides once and index cancelled/moved originals by program|date so we can skip them.
        List<LessonOverride> allOverrides = overrides.findInRange(from, to);
        Set<String> suppressed = new HashSet<>(); // program|originalDate that should NOT be generated
        for (LessonOverride o : allOverrides) {
            if ((o.getType() == LessonOverrideType.CANCELLED || o.getType() == LessonOverrideType.MOVED)
                    && o.getProgramId() != null && o.getOriginalDate() != null) {
                suppressed.add(o.getProgramId() + "|" + o.getOriginalDate());
            }
        }

        // Trainers see only their assigned programs (prd §7.5); owner/admin see all internally visible (incl. hidden).
        List<Program> source = SecurityUtils.isPrivileged()
                ? programs.findInternallyVisibleWithLocation()
                : programs.findByTrainer(SecurityUtils.currentUserId());

        for (Program p : source) {
            // Hidden programs are shown internally (schedule/shifts); only archived are excluded.
            if (p.getStatus() == ProgramStatus.ARCHIVED) {
                continue;
            }
            // Only recurring lessons appear in the schedule (camps are date-range, not weekly).
            if (p.getType() != ProgramType.CLUB && p.getType() != ProgramType.SCHOOL) {
                continue;
            }
            if (p.getWeekday() == null || p.getTime() == null || p.getTime().isBlank()) {
                continue;
            }
            if (locationId != null && !locationId.isBlank()
                    && (p.getLocation() == null || !locationId.equals(p.getLocation().getId()))) {
                continue;
            }

            LocalTime start = parseTime(p.getTime());
            if (start == null) {
                continue;
            }
            Integer duration = p.getDurationMin();
            String end = duration != null ? start.plusMinutes(duration).format(HHMM) : null;

            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                if (date.getDayOfWeek().getValue() != p.getWeekday()) {
                    continue;
                }
                if (p.getValidFrom() != null && date.isBefore(p.getValidFrom())) {
                    continue;
                }
                if (p.getValidTo() != null && date.isAfter(p.getValidTo())) {
                    continue;
                }
                // Skip occurrences cancelled or moved away from this date.
                if (suppressed.contains(p.getId() + "|" + date)) {
                    continue;
                }

                entries.add(new ScheduleEntryDto(
                        p.getId(),
                        p.getName(),
                        p.getType().name().toLowerCase(),
                        p.getStatus().name().toLowerCase(),
                        date,
                        p.getWeekday(),
                        start.format(HHMM),
                        end,
                        duration,
                        p.getSchoolPart() == null ? null : p.getSchoolPart().name().toLowerCase(),
                        p.getLocation() == null ? null : p.getLocation().getId(),
                        p.getLocation() == null ? null : p.getLocation().getName(),
                        p.getLocation() == null ? null : p.getLocation().getAddress(),
                        p.getLocation() == null ? null : p.getLocation().getContactName(),
                        p.getLocation() == null ? null : p.getLocation().getContactPhone(),
                        p.getLocation() == null ? null : p.getLocation().getContactEmail(),
                        p.getValidFrom(),
                        p.getValidTo(),
                        p.getCapacity(),
                        p.getSpotsTaken(),
                        null,
                        null,
                        null
                ));
            }
        }

        // Apply MOVED (new slot) and ONE_OFF overrides.
        Map<String, Program> programById = new HashMap<>();
        Map<String, Location> locationById = new HashMap<>();
        for (LessonOverride o : allOverrides) {
            if (o.getType() == LessonOverrideType.CANCELLED) {
                continue; // pure removal, handled by suppressed set
            }
            LocalDate d = o.getDate();
            if (d == null || d.isBefore(from) || d.isAfter(to)) {
                continue;
            }
            Program p = o.getProgramId() == null ? null
                    : programById.computeIfAbsent(o.getProgramId(),
                            id -> programs.findById(id).orElse(null));
            // Trainers only see overrides for their programs (or program-less one-offs stay visible to all).
            if (!SecurityUtils.isPrivileged() && p != null
                    && !programs.isTrainerAssigned(p.getId(), SecurityUtils.currentUserId())) {
                continue;
            }
            String locId = o.getLocationId() != null ? o.getLocationId()
                    : (p != null && p.getLocation() != null ? p.getLocation().getId() : null);
            Location loc = locId == null ? null
                    : locationById.computeIfAbsent(locId, id -> locations.findById(id).orElse(null));
            if (locationId != null && !locationId.isBlank()
                    && (loc == null || !locationId.equals(loc.getId()))) {
                continue;
            }
            LocalTime start = parseTime(o.getTime());
            Integer duration = o.getDurationMin() != null ? o.getDurationMin()
                    : (p != null ? p.getDurationMin() : null);
            String end = (start != null && duration != null) ? start.plusMinutes(duration).format(HHMM) : null;
            String name = o.getTitle() != null && !o.getTitle().isBlank() ? o.getTitle()
                    : (p != null ? p.getName() : "Jednorázová akce");
            String type = p != null ? p.getType().name().toLowerCase() : "club";

            entries.add(new ScheduleEntryDto(
                    p != null ? p.getId() : null,
                    name,
                    type,
                    p != null ? p.getStatus().name().toLowerCase() : "active",
                    d,
                    d.getDayOfWeek().getValue(),
                    start != null ? start.format(HHMM) : null,
                    end,
                    duration,
                    p != null && p.getSchoolPart() != null ? p.getSchoolPart().name().toLowerCase() : null,
                    loc == null ? null : loc.getId(),
                    loc == null ? null : loc.getName(),
                    loc == null ? null : loc.getAddress(),
                    loc == null ? null : loc.getContactName(),
                    loc == null ? null : loc.getContactPhone(),
                    loc == null ? null : loc.getContactEmail(),
                    null,
                    null,
                    p != null ? p.getCapacity() : null,
                    p != null ? p.getSpotsTaken() : 0,
                    o.getId(),
                    o.getType().name().toLowerCase(),
                    o.getTitle()
            ));
        }

        entries.sort(Comparator
                .comparing(ScheduleEntryDto::date)
                .thenComparing(e -> e.startTime() == null ? "" : e.startTime()));
        return entries;
    }

    private LocalTime parseTime(String hhmm) {
        try {
            return LocalTime.parse(hhmm.trim(), HHMM);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lists concrete dated occurrences of a single recurring program within a range.
     * Used by shift-signup to offer free lessons (prd §7.4). No role scoping here.
     */
    @Transactional(readOnly = true)
    public List<LocalDate> occurrenceDates(Program p, LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        if (p.getType() != ProgramType.CLUB && p.getType() != ProgramType.SCHOOL) {
            return dates;
        }
        if (p.getWeekday() == null || p.getTime() == null || p.getTime().isBlank()) {
            return dates;
        }
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() != p.getWeekday()) {
                continue;
            }
            if (p.getValidFrom() != null && date.isBefore(p.getValidFrom())) {
                continue;
            }
            if (p.getValidTo() != null && date.isAfter(p.getValidTo())) {
                continue;
            }
            dates.add(date);
        }
        return dates;
    }
}
