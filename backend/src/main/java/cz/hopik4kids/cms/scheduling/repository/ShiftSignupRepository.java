package cz.hopik4kids.cms.scheduling.repository;

import cz.hopik4kids.cms.scheduling.domain.ShiftSignup;
import cz.hopik4kids.cms.scheduling.domain.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShiftSignupRepository extends JpaRepository<ShiftSignup, String> {

    Optional<ShiftSignup> findByProgramIdAndLessonDateAndTrainerId(String programId, LocalDate lessonDate, String trainerId);

    List<ShiftSignup> findByTrainerIdAndLessonDateGreaterThanEqualOrderByLessonDateAsc(String trainerId, LocalDate from);

    List<ShiftSignup> findByProgramIdAndLessonDate(String programId, LocalDate lessonDate);

    /** Signups in a date range, optionally filtered by status, for occupancy counting. */
    @Query("select s from ShiftSignup s where s.lessonDate between :from and :to")
    List<ShiftSignup> findInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByProgramIdAndLessonDateAndStatusIn(String programId, LocalDate lessonDate, List<ShiftStatus> statuses);

    /** Distinct program ids where the trainer has a signup with any of the given statuses. */
    @Query("select distinct s.programId from ShiftSignup s where s.trainerId = :trainerId and s.status in :statuses")
    List<String> findProgramIdsByTrainerAndStatusIn(@Param("trainerId") String trainerId,
                                                     @Param("statuses") List<ShiftStatus> statuses);
}
