package cz.hopik4kids.cms.billing.repository;

import cz.hopik4kids.cms.billing.domain.SchoolContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolContractRepository extends JpaRepository<SchoolContract, String> {

    Optional<SchoolContract> findByProgramId(String programId);

    List<SchoolContract> findAllByOrderByCreatedAtAsc();
}
