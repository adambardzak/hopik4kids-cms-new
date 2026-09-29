package cz.hopik4kids.cms.billing.repository;

import cz.hopik4kids.cms.billing.domain.CreditNoteCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CreditNoteCounterRepository extends JpaRepository<CreditNoteCounter, Integer> {

    /** Locks the year's counter row for atomic increment (no duplicate credit-note numbers). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CreditNoteCounter c where c.year = :year")
    Optional<CreditNoteCounter> findByYearForUpdate(@Param("year") int year);
}
