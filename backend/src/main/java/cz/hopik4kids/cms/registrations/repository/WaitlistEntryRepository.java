package cz.hopik4kids.cms.registrations.repository;

import cz.hopik4kids.cms.registrations.domain.WaitlistEntry;
import cz.hopik4kids.cms.registrations.domain.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, String> {

    List<WaitlistEntry> findByProgramIdOrderByCreatedAtAsc(String programId);

    List<WaitlistEntry> findByProgramIdAndStatusOrderByCreatedAtAsc(String programId, WaitlistStatus status);

    long countByProgramIdAndStatus(String programId, WaitlistStatus status);
}
