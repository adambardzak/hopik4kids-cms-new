package cz.hopik4kids.cms.core.web.dto;

import cz.hopik4kids.cms.core.domain.Program;

import java.time.LocalDate;

/** Admin program view - includes status and whether an access code is set (never the code itself). */
public record AdminProgramDto(
        String id,
        String type,
        String name,
        String slug,
        String locationId,
        int price,
        Integer capacity,
        int spotsTaken,
        String accessMode,
        String restrictionNote,
        boolean hasAccessCode,
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
    public static AdminProgramDto from(Program p) {
        return new AdminProgramDto(
                p.getId(),
                p.getType().name().toLowerCase(),
                p.getName(),
                p.getSlug(),
                p.getLocation() == null ? null : p.getLocation().getId(),
                p.getPrice(),
                p.getCapacity(),
                p.getSpotsTaken(),
                p.getAccessMode().name().toLowerCase(),
                p.getRestrictionNote(),
                p.getAccessCodeHash() != null,
                p.getShirtPolicy().name().toLowerCase(),
                p.getStatus().name().toLowerCase(),
                p.getWeekday(),
                p.getTime(),
                p.getSchoolPart() == null ? null : p.getSchoolPart().name().toLowerCase(),
                p.getValidFrom(),
                p.getValidTo(),
                p.getDurationMin(),
                p.getStartDate(),
                p.getEndDate()
        );
    }
}
