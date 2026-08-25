package cz.hopik4kids.cms.core.repository;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramStatus;
import cz.hopik4kids.cms.core.domain.ProgramType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgramRepository extends JpaRepository<Program, String> {

    List<Program> findByTypeAndStatus(ProgramType type, ProgramStatus status);

    List<Program> findByStatus(ProgramStatus status);

    @Query("select p from Program p left join fetch p.location where p.status = :status")
    List<Program> findByStatusWithLocation(@Param("status") ProgramStatus status);

    @Query("select p from Program p left join fetch p.location where p.type = :type and p.status = :status")
    List<Program> findByTypeAndStatusWithLocation(@Param("type") ProgramType type,
                                                  @Param("status") ProgramStatus status);

    @Query("select p from Program p left join fetch p.location where p.id = :id")
    Optional<Program> findByIdWithLocation(@Param("id") String id);

    Optional<Program> findBySlug(String slug);

    /**
     * Pessimistic row lock for the capacity check + spotsTaken mutation (prd §4.2) -
     * prevents overbooking under concurrent registration.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Program p where p.id = :id")
    Optional<Program> findByIdForUpdate(@Param("id") String id);
}
