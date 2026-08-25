package cz.hopik4kids.cms.registrations.domain;

import cz.hopik4kids.cms.kernel.crypto.EncryptedStringConverter;
import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Child (prd §3B.3, variant A - deduplicated personal data enabling retention / cross-sell).
 * {@code personalId} (Czech RČ) is sensitive: encrypted at-rest (prd §3B.9, §9.3) and never
 * exposed on the public API.
 */
@Entity
@Table(name = "child")
public class Child extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String personalId;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String healthInsurance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getPersonalId() {
        return personalId;
    }

    public void setPersonalId(String personalId) {
        this.personalId = personalId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getHealthInsurance() {
        return healthInsurance;
    }

    public void setHealthInsurance(String healthInsurance) {
        this.healthInsurance = healthInsurance;
    }

    public Parent getParent() {
        return parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }
}
