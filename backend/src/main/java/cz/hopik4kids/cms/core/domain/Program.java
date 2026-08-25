package cz.hopik4kids.cms.core.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Everything one can register for (prd §3B.1): unifies the old Course + Camp.
 * Distinguished by {@link ProgramType}. Type-specific fields are nullable and validated
 * in the service layer.
 * <p>
 * {@code spotsTaken} is a denormalized counter mutated transactionally on registration
 * create/cancel (prd §3B.9, §4.2) - never computed by scanning registrations.
 * {@code accessCode} is stored hashed (prd §3B.10).
 */
@Entity
@Table(name = "program")
public class Program extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramType type;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(nullable = false)
    private int price;

    /** Max children; null = unlimited. */
    @Column
    private Integer capacity;

    /** Denormalized count of active registrations (prd §3B.9). */
    @Column(nullable = false)
    private int spotsTaken = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccessMode accessMode = AccessMode.PUBLIC;

    @Column(columnDefinition = "text")
    private String restrictionNote;

    /** Hashed access code, only for accessMode = CODE. */
    @Column
    private String accessCodeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShirtPolicy shirtPolicy = ShirtPolicy.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramStatus status = ProgramStatus.ACTIVE;

    // --- club / school ---

    /** ISO day of week 1..7 (Mon..Sun). */
    @Column
    private Integer weekday;

    /** Lesson time "HH:MM". */
    @Column(length = 5)
    private String time;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SchoolPart schoolPart;

    // --- scheduling (prd §6A.8, phase 3; nullable now) ---

    @Column
    private LocalDate validFrom;

    @Column
    private LocalDate validTo;

    @Column
    private Integer durationMin;

    // --- camp ---

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    // --- getters / setters ---

    public ProgramType getType() {
        return type;
    }

    public void setType(ProgramType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public int getSpotsTaken() {
        return spotsTaken;
    }

    public void setSpotsTaken(int spotsTaken) {
        this.spotsTaken = spotsTaken;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(AccessMode accessMode) {
        this.accessMode = accessMode;
    }

    public String getRestrictionNote() {
        return restrictionNote;
    }

    public void setRestrictionNote(String restrictionNote) {
        this.restrictionNote = restrictionNote;
    }

    public String getAccessCodeHash() {
        return accessCodeHash;
    }

    public void setAccessCodeHash(String accessCodeHash) {
        this.accessCodeHash = accessCodeHash;
    }

    public ShirtPolicy getShirtPolicy() {
        return shirtPolicy;
    }

    public void setShirtPolicy(ShirtPolicy shirtPolicy) {
        this.shirtPolicy = shirtPolicy;
    }

    public ProgramStatus getStatus() {
        return status;
    }

    public void setStatus(ProgramStatus status) {
        this.status = status;
    }

    public Integer getWeekday() {
        return weekday;
    }

    public void setWeekday(Integer weekday) {
        this.weekday = weekday;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public SchoolPart getSchoolPart() {
        return schoolPart;
    }

    public void setSchoolPart(SchoolPart schoolPart) {
        this.schoolPart = schoolPart;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public Integer getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(Integer durationMin) {
        this.durationMin = durationMin;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
