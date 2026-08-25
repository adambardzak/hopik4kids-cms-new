package cz.hopik4kids.cms.registrations.web.dto;

import cz.hopik4kids.cms.registrations.domain.WaitlistEntry;

import java.time.Instant;

public record WaitlistEntryDto(
        String id,
        String programId,
        String programName,
        String childName,
        String parentName,
        String parentPhone,
        String parentEmail,
        String note,
        String status,
        Instant createdAt
) {
    public static WaitlistEntryDto from(WaitlistEntry w) {
        return new WaitlistEntryDto(
                w.getId(),
                w.getProgram().getId(),
                w.getProgram().getName(),
                w.getChildName(),
                w.getParentName(),
                w.getParentPhone(),
                w.getParentEmail(),
                w.getNote(),
                w.getStatus().name().toLowerCase(),
                w.getCreatedAt());
    }
}
