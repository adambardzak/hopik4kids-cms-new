package cz.hopik4kids.cms.scheduling.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A timesheet entry for part-time staff (prd todo #3). A trainer records hours worked on a day;
 * an admin approves. Entries may be seeded from approved shift signups (source = SHIFT) and then
 * edited. Optional program reference for context.
 */
@Entity
@Table(name = "work_log")
public class WorkLog extends BaseEntity {

    @Column(name = "trainer_id", nullable = false)
    private String trainerId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal hours;

    @Column(columnDefinition = "text")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkLogSource source = WorkLogSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkLogStatus status = WorkLogStatus.PENDING;

    @Column(name = "program_id")
    private String programId;

    public String getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(String trainerId) {
        this.trainerId = trainerId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public WorkLogSource getSource() {
        return source;
    }

    public void setSource(WorkLogSource source) {
        this.source = source;
    }

    public WorkLogStatus getStatus() {
        return status;
    }

    public void setStatus(WorkLogStatus status) {
        this.status = status;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }
}
