package cz.hopik4kids.cms.core.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Venue where programs take place (prd §3B.2): kindergarten, school or sport venue. Shared, optional on a program. */
@Entity
@Table(name = "location")
public class Location extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocationKind kind;

    @Column
    private String address;

    /** Contact person at the venue (e.g. kindergarten coordinator) — prd §6A.8 request. */
    @Column
    private String contactName;

    @Column
    private String contactPhone;

    @Column
    private String contactEmail;

    @Column(columnDefinition = "text")
    private String note;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocationKind getKind() {
        return kind;
    }

    public void setKind(LocationKind kind) {
        this.kind = kind;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
