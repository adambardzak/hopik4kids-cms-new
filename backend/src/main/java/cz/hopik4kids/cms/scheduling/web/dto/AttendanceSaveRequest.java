package cz.hopik4kids.cms.scheduling.web.dto;

import java.util.List;

/** Bulk save of attendance for one lesson (program + date). */
public record AttendanceSaveRequest(List<Entry> entries) {
    public record Entry(String childId, String status, String note) {
    }
}
