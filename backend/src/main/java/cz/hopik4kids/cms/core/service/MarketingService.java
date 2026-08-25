package cz.hopik4kids.cms.core.service;

import cz.hopik4kids.cms.core.domain.ProgramType;
import cz.hopik4kids.cms.core.web.dto.MarketingStatsDto;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Marketing & growth metrics (prd §6A.2): registration sources, retention (returning children),
 * cross-sell (club kids not yet in a camp). Child matching is by normalized full name
 * (personalId is encrypted and can't be grouped in SQL).
 */
@Service
public class MarketingService {

    private final RegistrationRepository registrations;

    public MarketingService(RegistrationRepository registrations) {
        this.registrations = registrations;
    }

    @Transactional(readOnly = true)
    public MarketingStatsDto stats() {
        // --- sources ---
        List<MarketingStatsDto.SourceCount> sources = new ArrayList<>();
        long withoutSource = 0;
        for (Object[] row : registrations.countBySource()) {
            String source = (String) row[0];
            long count = ((Number) row[1]).longValue();
            if (source == null || source.isBlank()) {
                withoutSource += count;
            } else {
                sources.add(new MarketingStatsDto.SourceCount(source, count));
            }
        }
        sources.sort(Comparator.comparingLong(MarketingStatsDto.SourceCount::count).reversed());

        // --- retention & cross-sell (name-keyed) ---
        List<Registration> all = registrations.findAllActiveWithDetails();

        // childKey -> set of program ids (retention = key in >1 program)
        Map<String, Set<String>> childPrograms = new HashMap<>();
        // childKey -> whether they are in any camp
        Map<String, Boolean> childInCamp = new HashMap<>();
        // childKey -> a representative registration (for contact in cross-sell)
        Map<String, Registration> repr = new HashMap<>();

        for (Registration r : all) {
            String key = nameKey(r.getChild().getFullName());
            childPrograms.computeIfAbsent(key, k -> new HashSet<>()).add(r.getProgram().getId());
            boolean isCamp = r.getProgram().getType() == ProgramType.CAMP;
            childInCamp.merge(key, isCamp, (a, b) -> a || b);
            repr.putIfAbsent(key, r);
        }

        long distinct = childPrograms.size();
        long returning = childPrograms.values().stream().filter(s -> s.size() > 1).count();
        int retentionPct = distinct == 0 ? 0 : (int) Math.round(100.0 * returning / distinct);

        // cross-sell: children in a club/school but not in any camp
        List<MarketingStatsDto.CrossSellChild> clubsNotInCamp = new ArrayList<>();
        long campChildren = 0;
        for (Map.Entry<String, Boolean> e : childInCamp.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                campChildren++;
            } else {
                Registration r = repr.get(e.getKey());
                var child = r.getChild();
                var parent = child.getParent();
                clubsNotInCamp.add(new MarketingStatsDto.CrossSellChild(
                        child.getFullName(), parent.getName(), parent.getPhone(), parent.getEmail()));
            }
        }
        clubsNotInCamp.sort(Comparator.comparing(MarketingStatsDto.CrossSellChild::childName));

        return new MarketingStatsDto(
                sources, withoutSource,
                returning, distinct, retentionPct,
                clubsNotInCamp, campChildren);
    }

    private static String nameKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
