package cz.hopik4kids.cms.billing.service;

import cz.hopik4kids.cms.billing.domain.InvoiceCounter;
import cz.hopik4kids.cms.billing.repository.InvoiceCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Generates atomic per-year invoice numbers like {@code 2026-0042} (prd §6A.5). */
@Service
public class InvoiceNumberService {

    private final InvoiceCounterRepository counters;

    public InvoiceNumberService(InvoiceCounterRepository counters) {
        this.counters = counters;
    }

    /** Next invoice number for the current year. Row-locked to prevent duplicates under concurrency. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        int year = LocalDate.now().getYear();
        InvoiceCounter counter = counters.findByYearForUpdate(year).orElseGet(() -> {
            InvoiceCounter c = new InvoiceCounter();
            c.setYear(year);
            c.setLastNumber(0);
            return counters.save(c);
        });
        int seq = counter.getLastNumber() + 1;
        counter.setLastNumber(seq);
        counters.save(counter);
        return String.format("%d-%04d", year, seq);
    }
}
