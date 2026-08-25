package cz.hopik4kids.cms.core.web.dto;

import java.util.List;

/** Marketing & growth metrics (prd §6A.2). */
public record MarketingStatsDto(
        // Registration sources — "odkud jste se dozvěděli" / UTM.
        List<SourceCount> sources,
        long registrationsWithoutSource,
        // Retention — children appearing in more than one program.
        long returningChildren,
        long totalDistinctChildren,
        int retentionPct,
        // Cross-sell — children in clubs/school but NOT in any camp.
        List<CrossSellChild> clubsNotInCamp,
        long campChildren
) {
    public record SourceCount(String source, long count) {
    }

    public record CrossSellChild(String childName, String parentName, String parentPhone, String parentEmail) {
    }
}
