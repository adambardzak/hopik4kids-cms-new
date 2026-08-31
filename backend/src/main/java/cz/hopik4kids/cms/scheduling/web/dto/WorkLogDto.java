package cz.hopik4kids.cms.scheduling.web.dto;

import cz.hopik4kids.cms.scheduling.domain.WorkLog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WorkLogDto(
        String id,
        String trainerId,
        String trainerName,
        LocalDate workDate,
        BigDecimal hours,
        String note,
        String source,
        String status,
        String programId,
        String programName,
        Instant createdAt
) {
    public static WorkLogDto from(WorkLog w, String trainerName, String programName) {
        return new WorkLogDto(
                w.getId(),
                w.getTrainerId(),
                trainerName,
                w.getWorkDate(),
                w.getHours(),
                w.getNote(),
                w.getSource().name().toLowerCase(),
                w.getStatus().name().toLowerCase(),
                w.getProgramId(),
                programName,
                w.getCreatedAt());
    }
}
