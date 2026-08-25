package cz.hopik4kids.cms.core.service;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramStatus;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.core.web.dto.DashboardStatsDto;
import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.domain.RegistrationStatus;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Aggregates dashboard metrics (prd §6A.1) from data already in the system. */
@Service
public class DashboardService {

    private final ProgramRepository programs;
    private final RegistrationRepository registrations;
    private final int underfillThresholdPct;
    private final ZoneId zone = ZoneId.of("Europe/Prague");

    public DashboardService(ProgramRepository programs,
                            RegistrationRepository registrations,
                            @Value("${app.dashboard.underfill-threshold-pct:40}") int underfillThresholdPct) {
        this.programs = programs;
        this.registrations = registrations;
        this.underfillThresholdPct = underfillThresholdPct;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto stats() {
        var todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        var weekStart = LocalDate.now(zone).with(DayOfWeek.MONDAY).atStartOfDay(zone).toInstant();

        long today = registrations.countByStatusAndCreatedAtGreaterThanEqual(RegistrationStatus.ACTIVE, todayStart);
        long week = registrations.countByStatusAndCreatedAtGreaterThanEqual(RegistrationStatus.ACTIVE, weekStart);
        long totalActive = registrations.countByStatus(RegistrationStatus.ACTIVE);

        List<Program> active = programs.findByStatus(ProgramStatus.ACTIVE);

        long totalCapacity = 0;
        long totalSpots = 0;
        long potentialFromRemaining = 0;
        List<DashboardStatsDto.UnderfilledProgram> underfilled = new ArrayList<>();

        for (Program p : active) {
            totalSpots += p.getSpotsTaken();
            if (p.getCapacity() != null && p.getCapacity() > 0) {
                totalCapacity += p.getCapacity();
                int remaining = Math.max(0, p.getCapacity() - p.getSpotsTaken());
                potentialFromRemaining += (long) remaining * p.getPrice();

                int pct = (int) Math.round(100.0 * p.getSpotsTaken() / p.getCapacity());
                if (pct < underfillThresholdPct) {
                    underfilled.add(new DashboardStatsDto.UnderfilledProgram(
                            p.getId(), p.getName(), p.getType().name().toLowerCase(),
                            p.getCapacity(), p.getSpotsTaken(), pct));
                }
            }
        }
        underfilled.sort(Comparator.comparingInt(DashboardStatsDto.UnderfilledProgram::occupancyPct));

        long confirmedRevenue = registrations.sumPriceByPaymentStatus(PaymentStatus.PAID);
        long unpaidAmount = registrations.sumPriceByPaymentStatus(PaymentStatus.UNPAID);
        long unpaidCount = registrations.countActiveByPaymentStatus(PaymentStatus.UNPAID);
        long expectedRevenue = confirmedRevenue + unpaidAmount;
        long potentialRevenue = expectedRevenue + potentialFromRemaining;

        long withoutMediaConsent = registrations.countActiveByConsentMedia(false);

        return new DashboardStatsDto(
                today, week, active.size(), totalActive,
                totalCapacity, totalSpots,
                confirmedRevenue, expectedRevenue, potentialRevenue,
                unpaidCount, unpaidAmount,
                withoutMediaConsent,
                underfilled);
    }
}
