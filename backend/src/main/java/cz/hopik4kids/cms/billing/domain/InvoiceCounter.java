package cz.hopik4kids.cms.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Per-year invoice sequence. Row is pessimistically locked on increment (atomic numbering). */
@Entity
@Table(name = "invoice_counter")
public class InvoiceCounter {

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
