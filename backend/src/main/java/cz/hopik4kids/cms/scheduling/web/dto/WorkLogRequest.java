package cz.hopik4kids.cms.scheduling.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Create/update a work-log entry (prd todo #3). */
public record WorkLogRequest(
        @NotNull LocalDate workDate,
        @NotNull @PositiveOrZero BigDecimal hours,
        String note,
        String programId
) {
}
