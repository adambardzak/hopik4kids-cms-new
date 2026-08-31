package cz.hopik4kids.cms.billing.repository;

import cz.hopik4kids.cms.billing.domain.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, String> {

    Optional<BankTransaction> findByTxId(String txId);

    List<BankTransaction> findAllByOrderByTxDateDescCreatedAtDesc();
}
