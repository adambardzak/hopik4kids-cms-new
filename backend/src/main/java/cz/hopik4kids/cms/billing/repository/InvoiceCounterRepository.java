package cz.hopik4kids.cms.billing.repository;

import cz.hopik4kids.cms.billing.domain.InvoiceCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounter, Integer> {

    /** Locks the year's counter row for atomic increment (no duplicate invoice numbers). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InvoiceCounter c where c.year = :year")
    Optional<InvoiceCounter> findByYearForUpdate(@Param("year") int year);
}
