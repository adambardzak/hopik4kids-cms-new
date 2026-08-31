package cz.hopik4kids.cms.scheduling.repository;

import cz.hopik4kids.cms.scheduling.domain.WorkLog;
import cz.hopik4kids.cms.scheduling.domain.WorkLogSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkLogRepository extends JpaRepository<WorkLog, String> {

    List<WorkLog> findByTrainerIdOrderByWorkDateDesc(String trainerId);

    List<WorkLog> findByTrainerIdAndWorkDateBetweenOrderByWorkDateDesc(String trainerId, LocalDate from, LocalDate to);

    List<WorkLog> findAllByOrderByWorkDateDesc();

    List<WorkLog> findByWorkDateBetweenOrderByWorkDateDesc(LocalDate from, LocalDate to);

    Optional<WorkLog> findByTrainerIdAndProgramIdAndWorkDateAndSource(
            String trainerId, String programId, LocalDate workDate, WorkLogSource source);
}
