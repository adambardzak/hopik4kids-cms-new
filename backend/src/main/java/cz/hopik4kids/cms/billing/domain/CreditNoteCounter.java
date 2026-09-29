package cz.hopik4kids.cms.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Per-year credit-note sequence, separate from invoices (D-2026-0001). Row-locked on increment. */
@Entity
@Table(name = "credit_note_counter")
public class CreditNoteCounter {

    @Id
    private int year;

    @Column(name = "last_number", nullable = false)
    private int lastNumber;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getLastNumber() {
        return lastNumber;
    }

    public void setLastNumber(int lastNumber) {
        this.lastNumber = lastNumber;
    }
}
