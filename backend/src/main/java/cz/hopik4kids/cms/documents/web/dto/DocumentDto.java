package cz.hopik4kids.cms.documents.web.dto;

import cz.hopik4kids.cms.documents.domain.Document;

public record DocumentDto(
        String id,
        String title,
        String category,
        String fileUrl,
        String fileName,
        String content,
        String visibility,
        int sortOrder
) {
    public static DocumentDto from(Document d) {
        return new DocumentDto(
                d.getId(),
                d.getTitle(),
                d.getCategory().name().toLowerCase(),
                d.getFile() == null ? null : d.getFile().getUrl(),
                d.getFile() == null ? null : d.getFile().getAlt(),
                d.getContent(),
                d.getVisibility().name().toLowerCase(),
                d.getSortOrder());
    }
}
