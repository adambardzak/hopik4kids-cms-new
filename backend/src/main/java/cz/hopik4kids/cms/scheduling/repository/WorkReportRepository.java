package cz.hopik4kids.cms.scheduling.repository;

import cz.hopik4kids.cms.scheduling.domain.WorkReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkReportRepository extends JpaRepository<WorkReport, String> {

    Optional<WorkReport> findByUserIdAndPeriod(String userId, String period);

    List<WorkReport> findByPeriod(String period);
}
