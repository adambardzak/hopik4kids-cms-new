package cz.hopik4kids.cms.core.web.dto;

import cz.hopik4kids.cms.core.domain.Location;

public record PublicLocationDto(String id, String name, String kind, String address) {
    public static PublicLocationDto from(Location l) {
        return new PublicLocationDto(l.getId(), l.getName(), l.getKind().name().toLowerCase(), l.getAddress());
    }
}
