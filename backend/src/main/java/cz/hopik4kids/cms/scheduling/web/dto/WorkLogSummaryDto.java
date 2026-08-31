package cz.hopik4kids.cms.scheduling.web.dto;

import java.math.BigDecimal;

/** Per-trainer aggregate of approved (and pending) hours for a period (prd todo #3). */
public record WorkLogSummaryDto(
        String trainerId,
        String trainerName,
        BigDecimal approvedHours,
        BigDecimal pendingHours,
        int entryCount
) {
}
