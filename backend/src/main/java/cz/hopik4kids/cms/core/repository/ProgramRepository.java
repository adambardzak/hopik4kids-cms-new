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

    /** Internally visible programs (ACTIVE + HIDDEN) for schedule/shifts/attendance — excludes ARCHIVED. */
    @Query("select p from Program p left join fetch p.location where p.status <> cz.hopik4kids.cms.core.domain.ProgramStatus.ARCHIVED")
    List<Program> findInternallyVisibleWithLocation();

    @Query("select p from Program p left join fetch p.location where p.type = :type and p.status = :status")
    List<Program> findByTypeAndStatusWithLocation(@Param("type") ProgramType type,
                                                  @Param("status") ProgramStatus status);

    @Query("select p from Program p left join fetch p.location where p.id = :id")
    Optional<Program> findByIdWithLocation(@Param("id") String id);

    Optional<Program> findBySlug(String slug);

    /** Programs a trainer is assigned to. */
    @Query("select p from Program p left join fetch p.location where :trainerId member of p.trainerIds")
    List<Program> findByTrainer(@Param("trainerId") String trainerId);

    /** Whether a trainer is assigned to a program. */
    @Query("select count(p) > 0 from Program p where p.id = :programId and :trainerId member of p.trainerIds")
    boolean isTrainerAssigned(@Param("programId") String programId, @Param("trainerId") String trainerId);

    /**
     * Pessimistic row lock for the capacity check + spotsTaken mutation (prd §4.2) -
     * prevents overbooking under concurrent registration.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Program p where p.id = :id")
    Optional<Program> findByIdForUpdate(@Param("id") String id);
}
