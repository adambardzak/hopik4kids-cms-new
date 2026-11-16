package cz.hopik4kids.cms.scheduling.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Monthly work report of one person. {@code status}: draft | submitted | approved | returned.
 * Totals are live (from the month's entries × current rate) until approved, then frozen.
 * {@code hourlyRate}/{@code totalAmount} are null when no rate is set.
 */
public record WorkReportDto(
        String userId,
        String userName,
        String role,
        String period,
        String status,
        BigDecimal totalHours,
        BigDecimal hourlyRate,
        BigDecimal totalAmount,
        int entryCount,
        int pendingCount,
        Instant submittedAt,
        Instant decidedAt,
        String decidedByName,
        String returnNote
) {
}
