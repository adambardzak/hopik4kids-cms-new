package cz.hopik4kids.cms.billing.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Supplier (Hopík4Kids) invoicing settings — a singleton row (prd §6A.5).
 * Used as the "dodavatel" block on invoices and for the QR payment (IBAN).
 */
@Entity
@Table(name = "supplier_settings")
public class SupplierSettings extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column
    private String ico;

    /** VAT id — only if Hopík becomes a VAT payer (currently not, prd §14.6). */
    @Column
    private String dic;

    @Column
    private String address;

    /** IBAN for the QR payment (SPAYD). */
    @Column
    private String iban;

    /** Human-readable account number (shown on the invoice). */
    @Column
    private String accountNumber;

    @Column
    private String web;

    @Column
    private String email;

    /** Default due-date offset in days (e.g. 14). */
    @Column(nullable = false)
    private int defaultDueDays = 14;

    @Column(columnDefinition = "text")
    private String footerText;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getDefaultDueDays() {
        return defaultDueDays;
    }

    public void setDefaultDueDays(int defaultDueDays) {
        this.defaultDueDays = defaultDueDays;
    }

    public String getFooterText() {
        return footerText;
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }
}
