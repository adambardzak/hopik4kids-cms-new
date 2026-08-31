package cz.hopik4kids.cms.records.web.dto;

import cz.hopik4kids.cms.records.domain.RecordDocument;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RecordDocumentDto(
        String id,
        String type,
        String title,
        String personId,
        String personName,
        LocalDate docDate,
        BigDecimal amount,
        String note,
        String originalName,
        String contentType,
        Long sizeBytes,
        Instant createdAt
) {
    public static RecordDocumentDto from(RecordDocument r, String resolvedPersonName) {
        return new RecordDocumentDto(
                r.getId(),
                r.getType().name().toLowerCase(),
                r.getTitle(),
                r.getPersonId(),
                resolvedPersonName,
                r.getDocDate(),
                r.getAmount(),
                r.getNote(),
                r.getOriginalName(),
                r.getContentType(),
                r.getSizeBytes(),
                r.getCreatedAt());
    }
}
