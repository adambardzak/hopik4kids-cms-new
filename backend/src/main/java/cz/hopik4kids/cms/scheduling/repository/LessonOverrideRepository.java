package cz.hopik4kids.cms.scheduling.repository;

import cz.hopik4kids.cms.scheduling.domain.LessonOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LessonOverrideRepository extends JpaRepository<LessonOverride, String> {

    /** All overrides whose effective or original date falls within the range. */
    @Query("""
            select o from LessonOverride o
            where (o.originalDate between :from and :to)
               or (o.date between :from and :to)
            """)
    List<LessonOverride> findInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
