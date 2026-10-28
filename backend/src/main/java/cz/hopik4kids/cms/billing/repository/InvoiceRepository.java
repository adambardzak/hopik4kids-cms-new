package cz.hopik4kids.cms.billing.repository;

import cz.hopik4kids.cms.billing.domain.Invoice;
import cz.hopik4kids.cms.billing.domain.InvoicePayerKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    Optional<Invoice> findByRegistrationId(String registrationId);

    List<Invoice> findAllByOrderByIssueDateDescInvoiceNumberDesc();

    List<Invoice> findByRegistrationIdIn(List<String> registrationIds);

    Optional<Invoice> findByVariableSymbol(String variableSymbol);

    List<Invoice> findBySchoolContractIdOrderByIssueDateDesc(String schoolContractId);

    List<Invoice> findByPayerKindOrderByIssueDateDescInvoiceNumberDesc(InvoicePayerKind payerKind);
}
