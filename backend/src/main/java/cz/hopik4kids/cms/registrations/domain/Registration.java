package cz.hopik4kids.cms.registrations.domain;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Single registration entity for clubs and camps (prd §3B.5) - unifies the old
 * Registration + CampRegistration. Variant A: references {@link Child} (and thus Parent).
 * <p>
 * {@code priceSnapshot} freezes the price at registration time (prd §3B.9, §4.5).
 * {@code status = CANCELLED} soft-deletes and decrements {@link Program#getSpotsTaken()}
 * transactionally (prd §4.9). Capacity is checked in the same transaction on create (prd §4.2).
 */
@Entity
@Table(name = "registration")
public class Registration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    /** Class within the kindergarten - only for clubs. */
    @Column
    private String className;

    @Column(nullable = false)
    private boolean wantsShirt;

    @Column(length = 20)
    private String shirtSize;

    @Column
    private String nickName;

    @Column(columnDefinition = "text")
    private String allergies;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false)
    private boolean consentPersonalData;

    @Column(nullable = false)
    private boolean consentMedia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    /** Frozen price = program.price + (wantsShirt ? 500 : 0). */
    @Column(nullable = false)
    private int priceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.ACTIVE;

    /** UTM / channel (prd §6A.2), optional. */
    @Column
    private String source;

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Child getChild() {
        return child;
    }

    public void setChild(Child child) {
        this.child = child;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isWantsShirt() {
        return wantsShirt;
    }

    public void setWantsShirt(boolean wantsShirt) {
        this.wantsShirt = wantsShirt;
    }

    public String getShirtSize() {
        return shirtSize;
    }

    public void setShirtSize(String shirtSize) {
        this.shirtSize = shirtSize;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isConsentPersonalData() {
        return consentPersonalData;
    }

    public void setConsentPersonalData(boolean consentPersonalData) {
        this.consentPersonalData = consentPersonalData;
    }

    public boolean isConsentMedia() {
        return consentMedia;
    }

    public void setConsentMedia(boolean consentMedia) {
        this.consentMedia = consentMedia;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public int getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(int priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
