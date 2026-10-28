package cz.hopik4kids.cms.billing.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Billing agreement with a kindergarten for a morning program ("faktury školky").
 * <p>
 * The lesson itself lives on the linked {@link cz.hopik4kids.cms.core.domain.Program}
 * (type=school, schoolPart=morning) — name, place, weekday, duration, period and trainers needed —
 * so the schedule, calendar, shifts and attendance keep working unchanged. This entity only adds
 * the invoicing side: who is billed, at what price and in what unit.
 */
@Entity
@Table(name = "school_contract")
public class SchoolContract extends BaseEntity {

    /** The Program this contract bills for (1:1). */
    @Column(nullable = false, unique = true)
    private String programId;

    /** Invoice recipient, when it differs from the program/kindergarten name. */
    @Column
    private String billingName;

    @Column
    private String billingEmail;

    @Column(length = 512)
    private String billingAddress;

    @Column(length = 50)
    private String ico;

    @Column(length = 50)
    private String dic;

    /** Agreed price in CZK, meaning given by {@link #priceUnit}. */
    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchoolPriceUnit priceUnit = SchoolPriceUnit.LESSON;

    @Column(columnDefinition = "text")
    private String note;

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getBillingName() {
        return billingName;
    }

    public void setBillingName(String billingName) {
        this.billingName = billingName;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public void setBillingEmail(String billingEmail) {
        this.billingEmail = billingEmail;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getIco() {
        return ico;
    }

    public void setIco(String ico) {
        this.ico = ico;
    }

    public String getDic() {
        return dic;
    }

    public void setDic(String dic) {
        this.dic = dic;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public SchoolPriceUnit getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(SchoolPriceUnit priceUnit) {
        this.priceUnit = priceUnit;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
