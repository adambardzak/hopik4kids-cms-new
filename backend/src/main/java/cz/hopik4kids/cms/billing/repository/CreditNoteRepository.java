package cz.hopik4kids.cms.billing.repository;

import cz.hopik4kids.cms.billing.domain.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditNoteRepository extends JpaRepository<CreditNote, String> {

    List<CreditNote> findAllByOrderByIssueDateDescNumberDesc();

    Optional<CreditNote> findByInvoiceId(String invoiceId);
}
