package cz.hopik4kids.cms.scheduling.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * An override to the generated schedule (prd §7.4 extension):
 * cancel a recurring lesson on a date, move it, or add a one-off lesson/event.
 *
 * <ul>
 *   <li>CANCELLED: {@code programId} + {@code originalDate} — that occurrence disappears.</li>
 *   <li>MOVED: {@code programId} + {@code originalDate} — occurrence hidden on original date and
 *       re-shown on {@code date}/{@code time} (with optional {@code locationId}).</li>
 *   <li>ONE_OFF: standalone entry on {@code date}/{@code time}; {@code programId} optional
 *       (may reference a program as a substitute lesson, or null for an ad-hoc event).</li>
 * </ul>
 */
@Entity
@Table(name = "lesson_override")
public class LessonOverride extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonOverrideType type;

    /** Recurring program this override applies to (null only for ad-hoc ONE_OFF events). */
    @Column(name = "program_id")
    private String programId;

    /** For CANCELLED/MOVED: the original occurrence date being overridden. */
    @Column(name = "original_date")
    private LocalDate originalDate;

    /** For MOVED/ONE_OFF: the effective date of the (new) lesson. */
    @Column(name = "lesson_date")
    private LocalDate date;

    /** For MOVED/ONE_OFF: start time "HH:mm". */
    @Column(length = 5)
    private String time;

    @Column(name = "duration_min")
    private Integer durationMin;

    /** For ONE_OFF without a program: a display title. */
    @Column
    private String title;

    /** Optional venue override. */
    @Column(name = "location_id")
    private String locationId;

    @Column
    private String note;

    public LessonOverrideType getType() {
        return type;
    }

    public void setType(LessonOverrideType type) {
        this.type = type;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public LocalDate getOriginalDate() {
        return originalDate;
    }

    public void setOriginalDate(LocalDate originalDate) {
        this.originalDate = originalDate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(Integer durationMin) {
        this.durationMin = durationMin;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
