package cz.hopik4kids.cms.scheduling.repository;

import cz.hopik4kids.cms.scheduling.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, String> {

    List<AttendanceRecord> findByProgramIdAndLessonDate(String programId, LocalDate lessonDate);

    List<AttendanceRecord> findByProgramId(String programId);
}
