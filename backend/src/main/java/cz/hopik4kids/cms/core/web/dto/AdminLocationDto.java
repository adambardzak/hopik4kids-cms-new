package cz.hopik4kids.cms.core.web.dto;

import cz.hopik4kids.cms.core.domain.Location;

public record AdminLocationDto(
        String id,
        String name,
        String kind,
        String address,
        String contactName,
        String contactPhone,
        String contactEmail,
        String note
) {
    public static AdminLocationDto from(Location l) {
        return new AdminLocationDto(
                l.getId(),
                l.getName(),
                l.getKind().name().toLowerCase(),
                l.getAddress(),
                l.getContactName(),
                l.getContactPhone(),
                l.getContactEmail(),
                l.getNote());
    }
}
