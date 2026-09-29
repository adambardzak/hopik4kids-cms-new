package cz.hopik4kids.cms.scheduling.service;

import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.scheduling.domain.ShiftStatus;
import cz.hopik4kids.cms.scheduling.repository.ShiftSignupRepository;
import cz.hopik4kids.cms.scheduling.web.dto.ScheduleEntryDto;
import cz.hopik4kids.cms.usersrbac.domain.Role;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a personal iCal (RFC 5545) feed of a user's lessons, subscribed to via a token URL
 * (webcal). Owner/admin get all lessons; trainers get only their assigned programs (prd §7.5).
 */
@Service
public class CalendarService {

    private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");
    private static final DateTimeFormatter ICAL_DT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final UserRepository users;
    private final ScheduleService schedule;
    private final ShiftSignupRepository shiftSignups;

    public CalendarService(UserRepository users, ScheduleService schedule,
                           ShiftSignupRepository shiftSignups) {
        this.users = users;
        this.schedule = schedule;
        this.shiftSignups = shiftSignups;
    }

    @Transactional(readOnly = true)
    public String feedForToken(String token) {
        return feedForToken(token, false);
    }

    /**
     * @param onlyMine when true, a trainer's feed contains only the concrete shifts they signed up
     *                 for (APPROVED), not every occurrence of their programs. Ignored for owner/admin.
     */
    @Transactional(readOnly = true)
    public String feedForToken(String token, boolean onlyMine) {
        User user = users.findByCalendarToken(token)
                .orElseThrow(() -> ApiException.notFound("Kalendář nenalezen"));

        boolean privileged = user.getRole() == Role.OWNER || user.getRole() == Role.ADMIN;

        // Window: 30 days back … 180 days ahead (rolling).
        LocalDate from = LocalDate.now(PRAGUE).minusDays(30);
        LocalDate to = LocalDate.now(PRAGUE).plusDays(180);

        List<ScheduleEntryDto> entries = schedule.forRange(from, to, null, privileged, user.getId());

        // "Jen moje směny": keep only occurrences the trainer has an APPROVED signup for.
        // Signup.programId holds either the program id (recurring) or "override:<id>" (one-off);
        // match that against each schedule entry's (program/override, date).
        Set<String> mineKeys = null;
        if (onlyMine && !privileged) {
            mineKeys = new HashSet<>();
            for (var s : shiftSignups.findByTrainerIdAndLessonDateGreaterThanEqualOrderByLessonDateAsc(
                    user.getId(), from)) {
                if (s.getStatus() == ShiftStatus.APPROVED) {
                    mineKeys.add(s.getProgramId() + "|" + s.getLessonDate());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Hopik4Kids//CMS//CS\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("X-WR-CALNAME:Hopík4Kids — rozvrh\r\n");
        sb.append("X-WR-TIMEZONE:Europe/Prague\r\n");

        String stamp = ZonedDateTime.now(PRAGUE).format(ICAL_DT);

        for (ScheduleEntryDto e : entries) {
            // "Jen moje": skip occurrences the trainer isn't personally signed up for.
            if (mineKeys != null) {
                String key = (e.overrideId() != null ? "override:" + e.overrideId() : e.programId())
                        + "|" + e.date();
                if (!mineKeys.contains(key)) {
                    continue;
                }
            }
            LocalTime start = parse(e.startTime());
            if (start == null) {
                continue;
            }
            LocalTime end = parse(e.endTime());
            if (end == null) {
                end = start.plusMinutes(e.durationMin() != null ? e.durationMin() : 45);
            }
            ZonedDateTime startZ = ZonedDateTime.of(e.date(), start, PRAGUE);
            ZonedDateTime endZ = ZonedDateTime.of(e.date(), end, PRAGUE);

            String uid = (e.overrideId() != null ? e.overrideId()
                    : (e.programId() != null ? e.programId() : "oneoff"))
                    + "-" + e.date() + "@hopik4kids.cz";

            StringBuilder desc = new StringBuilder();
            if (e.locationName() != null) {
                desc.append("Místo: ").append(e.locationName());
            }
            if (e.capacity() != null) {
                if (desc.length() > 0) desc.append("\\n");
                desc.append("Obsazenost: ").append(e.spotsTaken()).append("/").append(e.capacity());
            }

            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:").append(uid).append("\r\n");
            sb.append("DTSTAMP:").append(stamp).append("\r\n");
            sb.append("DTSTART;TZID=Europe/Prague:").append(startZ.format(ICAL_DT)).append("\r\n");
            sb.append("DTEND;TZID=Europe/Prague:").append(endZ.format(ICAL_DT)).append("\r\n");
            sb.append("SUMMARY:").append(escape(e.programName())).append("\r\n");
            if (e.locationName() != null) {
                sb.append("LOCATION:").append(escape(e.locationName())).append("\r\n");
            }
            if (desc.length() > 0) {
                sb.append("DESCRIPTION:").append(escape(desc.toString())).append("\r\n");
            }
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private LocalTime parse(String hhmm) {
        if (hhmm == null || hhmm.isBlank()) return null;
        try {
            return LocalTime.parse(hhmm.trim(), HHMM);
        } catch (Exception ex) {
            return null;
        }
    }

    /** iCal text escaping (RFC 5545 §3.3.11). */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }
}
