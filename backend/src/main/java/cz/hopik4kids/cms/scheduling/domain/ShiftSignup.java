package cz.hopik4kids.cms.scheduling.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * A trainer signing up for a lesson occurrence (program + date) — prd §7.4.
 * Lessons are generated from programs (no LessonInstance rows), so the occurrence is
 * identified by (programId, date). One signup per trainer+program+date.
 */
@Entity
@Table(name = "shift_signup",
        uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "lesson_date", "trainer_id"}))
public class ShiftSignup extends BaseEntity {

    @Column(name = "program_id", nullable = false)
    private String programId;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "trainer_id", nullable = false)
    private String trainerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.PENDING;

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public LocalDate getLessonDate() {
        return lessonDate;
    }

    public void setLessonDate(LocalDate lessonDate) {
        this.lessonDate = lessonDate;
    }

    public String getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(String trainerId) {
        this.trainerId = trainerId;
    }

    public ShiftStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftStatus status) {
        this.status = status;
    }
}
