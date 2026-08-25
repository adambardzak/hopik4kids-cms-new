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
 * Attendance of a child at a specific lesson occurrence (program + date). One record per
 * child+program+date. Lessons are generated from programs (no LessonInstance rows), so the
 * occurrence is identified by (programId, date).
 */
@Entity
@Table(name = "attendance_record",
        uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "child_id", "lesson_date"}))
public class AttendanceRecord extends BaseEntity {

    @Column(name = "program_id", nullable = false)
    private String programId;

    @Column(name = "child_id", nullable = false)
    private String childId;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(columnDefinition = "text")
    private String note;

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public LocalDate getLessonDate() {
        return lessonDate;
    }

    public void setLessonDate(LocalDate lessonDate) {
        this.lessonDate = lessonDate;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
