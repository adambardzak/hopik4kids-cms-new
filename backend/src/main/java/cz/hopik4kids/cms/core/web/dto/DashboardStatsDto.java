package cz.hopik4kids.cms.core.web.dto;

import java.util.List;

/** Dashboard metrics (prd §6A.1). All amounts in CZK. */
public record DashboardStatsDto(
        long registrationsToday,
        long registrationsThisWeek,
        long activePrograms,
        long totalActiveRegistrations,
        // occupancy across active club/school/camp programs with a capacity limit
        long totalCapacity,
        long totalSpotsTaken,
        // revenue
        long confirmedRevenue,   // Σ priceSnapshot of PAID active registrations
        long expectedRevenue,    // Σ priceSnapshot of all active registrations (paid + unpaid)
        long potentialRevenue,   // expected + remaining capacity × program price
        long unpaidCount,
        long unpaidAmount,
        // GDPR: children without media consent (prd §6A.6)
        long withoutMediaConsent,
        // programs below the fill threshold (need promotion)
        List<UnderfilledProgram> underfilled
) {
    public record UnderfilledProgram(
            String id,
            String name,
            String type,
            int capacity,
            int spotsTaken,
            int occupancyPct
    ) {
    }
}
