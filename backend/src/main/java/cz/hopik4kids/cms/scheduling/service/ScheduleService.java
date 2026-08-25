package cz.hopik4kids.cms.scheduling.service;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramStatus;
import cz.hopik4kids.cms.core.domain.ProgramType;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.SecurityUtils;
import cz.hopik4kids.cms.scheduling.web.dto.ScheduleEntryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the weekly schedule (prd §6A.8 A) by expanding recurring club/school programs into
 * concrete dated occurrences for a date range. Used to see which slots are taken and therefore
 * which are free to offer to a kindergarten.
 */
@Service
public class ScheduleService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final ProgramRepository programs;

    public ScheduleService(ProgramRepository programs) {
        this.programs = programs;
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

        // Trainers see only their assigned programs (prd §7.5); owner/admin see all.
        List<Program> source = SecurityUtils.isPrivileged()
                ? programs.findByStatusWithLocation(ProgramStatus.ACTIVE)
                : programs.findByTrainer(SecurityUtils.currentUserId());

        for (Program p : source) {
            if (p.getStatus() != ProgramStatus.ACTIVE) {
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

                entries.add(new ScheduleEntryDto(
                        p.getId(),
                        p.getName(),
                        p.getType().name().toLowerCase(),
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
                        p.getSpotsTaken()
                ));
            }
        }

        entries.sort(Comparator
                .comparing(ScheduleEntryDto::date)
                .thenComparing(ScheduleEntryDto::startTime));
        return entries;
    }

    private LocalTime parseTime(String hhmm) {
        try {
            return LocalTime.parse(hhmm.trim(), HHMM);
        } catch (Exception e) {
            return null;
        }
    }
}
