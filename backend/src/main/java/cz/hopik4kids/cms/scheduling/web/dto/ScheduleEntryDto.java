package cz.hopik4kids.cms.scheduling.web.dto;

import java.time.LocalDate;

/**
 * A concrete occurrence of a recurring lesson on a specific date (prd §6A.8 A).
 * Generated on-the-fly from a {@code Program} for the requested week. Carries venue details
 * (address + contact person) and the program's validity period for the lesson detail view.
 */
public record ScheduleEntryDto(
        String programId,
        String programName,
        String type,
        String status,
        LocalDate date,
        int weekday,
        String startTime,
        String endTime,
        Integer durationMin,
        String schoolPart,
        String locationId,
        String locationName,
        String locationAddress,
        String contactName,
        String contactPhone,
        String contactEmail,
        LocalDate validFrom,
        LocalDate validTo,
        Integer capacity,
        int spotsTaken,
        String overrideId,
        String overrideType,
        String title
) {
}
