package cz.hopik4kids.cms.scheduling.web.dto;

import java.util.List;

/**
 * Attendance statistics for a program (prd §6A.3): per-child totals + per-lesson totals, plus the
 * lesson plan (held / remaining) from the schedule — independent of whether attendance is recorded.
 */
public record AttendanceStatsDto(
        List<PerChild> children,
        List<PerLesson> lessons,
        LessonPlan plan
) {
    /**
     * Scheduled lessons over the program's validity (cancellations excluded, moved/extra lessons
     * included). {@code done} = before today, {@code remaining} = today onwards (Prague time).
     */
    public record LessonPlan(
            int total,
            int done,
            int remaining,
            String nextDate,
            String validFrom,
            String validTo
    ) {
    }

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
