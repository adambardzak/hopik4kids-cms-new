package cz.hopik4kids.cms.documents.web.dto;

public record DocumentRequest(
        String title,
        String category,
        String fileId,
        String content,
        String visibility,
        Integer sortOrder
) {
}
