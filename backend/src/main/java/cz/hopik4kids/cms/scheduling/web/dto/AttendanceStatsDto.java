package cz.hopik4kids.cms.scheduling.web.dto;

import java.util.List;

/** Attendance statistics for a program (prd §6A.3): per-child totals + per-lesson totals. */
public record AttendanceStatsDto(
        List<PerChild> children,
        List<PerLesson> lessons
) {
    public record PerChild(
            String childId,
            String childName,
            int present,
            int excused,
            int absent,
            int totalRecorded
    ) {
    }

    public record PerLesson(
            String date,
            int present,
            int excused,
            int absent
    ) {
    }
}
