package cz.hopik4kids.cms.core.web.dto;

import java.time.LocalDate;

/** Admin create/update payload for a Program (prd §5.6, §6.4). accessCode is plaintext (hashed server-side). */
public record AdminProgramRequest(
        String type,
        String name,
        String slug,
        String locationId,
        Integer price,
        Integer capacity,
        String accessMode,
        String restrictionNote,
        String accessCode,
        String shirtPolicy,
        String status,
        Integer weekday,
        String time,
        String schoolPart,
        LocalDate validFrom,
        LocalDate validTo,
        Integer durationMin,
        LocalDate startDate,
        LocalDate endDate
) {
}
