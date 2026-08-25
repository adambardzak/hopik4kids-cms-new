package cz.hopik4kids.cms.scheduling.web.dto;

/** One row of the attendance roster for a lesson: a child + their current status/note. */
public record AttendanceRowDto(
        String childId,
        String childName,
        String status,   // present | excused | absent | null (not recorded yet)
        String note
) {
}
