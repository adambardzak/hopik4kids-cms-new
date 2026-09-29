package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.domain.CreditNoteCounter;
import cz.hopik4kids.cms.billing.repository.CreditNoteCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Generates atomic per-year credit-note numbers like {@code D-2026-0001} — a sequence independent
 * of invoices (a credit note for invoice 2026-0044 is D-2026-0001, not 44). Resets each year.
 */
@Service
public class CreditNoteNumberService {

    private final CreditNoteCounterRepository counters;

    public CreditNoteNumberService(CreditNoteCounterRepository counters) {
        this.counters = counters;
    }

    /** Next credit-note number for the current year. Row-locked to prevent duplicates. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        int year = LocalDate.now().getYear();
        CreditNoteCounter counter = counters.findByYearForUpdate(year).orElseGet(() -> {
            CreditNoteCounter c = new CreditNoteCounter();
            c.setYear(year);
            c.setLastNumber(0);
            return counters.save(c);
        });
        int seq = counter.getLastNumber() + 1;
        counter.setLastNumber(seq);
        counters.save(counter);
        return String.format("D-%d-%04d", year, seq);
    }
}
