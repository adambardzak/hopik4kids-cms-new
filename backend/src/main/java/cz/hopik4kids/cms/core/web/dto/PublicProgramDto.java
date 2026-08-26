package cz.hopik4kids.cms.core.web.dto;

import cz.hopik4kids.cms.core.domain.Program;

import java.time.LocalDate;

/**
 * Public program view (prd §5.2). Carries capacity + spotsTaken so the website computes
 * spotsLeft / isFull locally. No personal data. accessCodeHash is never exposed.
 */
public record PublicProgramDto(
        String id,
        String type,
        String name,
        String slug,
        Integer price,
        Integer capacity,
        int spotsTaken,
        String accessMode,
        String restrictionNote,
        String shirtPolicy,
        Integer weekday,
        String time,
        String schoolPart,
        Integer durationMin,
        LocalDate validFrom,
        LocalDate validTo,
        LocalDate startDate,
        LocalDate endDate,
        PublicLocationDto location
) {
    public static PublicProgramDto from(Program p, boolean includeLocation) {
        return new PublicProgramDto(
                p.getId(),
                p.getType().name().toLowerCase(),
                p.getName(),
                p.getSlug(),
                p.getPrice(),
                p.getCapacity(),
                p.getSpotsTaken(),
                p.getAccessMode().name().toLowerCase(),
                p.getRestrictionNote(),
                p.getShirtPolicy().name().toLowerCase(),
                p.getWeekday(),
                p.getTime(),
                p.getSchoolPart() == null ? null : p.getSchoolPart().name().toLowerCase(),
                p.getDurationMin(),
                p.getValidFrom(),
                p.getValidTo(),
                p.getStartDate(),
                p.getEndDate(),
                includeLocation && p.getLocation() != null ? PublicLocationDto.from(p.getLocation()) : null
        );
    }
}
