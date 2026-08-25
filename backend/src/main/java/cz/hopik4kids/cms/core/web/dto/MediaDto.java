package cz.hopik4kids.cms.core.web.dto;

import cz.hopik4kids.cms.core.domain.Media;

public record MediaDto(String id, String url, String alt, Integer width, Integer height) {
    public static MediaDto from(Media m) {
        return new MediaDto(m.getId(), m.getUrl(), m.getAlt(), m.getWidth(), m.getHeight());
    }
}
