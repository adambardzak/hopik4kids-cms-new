package cz.hopik4kids.cms.scheduling.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A lesson occurrence offered to trainers for shift-signup (prd §7.4).
 * Carries occupancy (how many trainers needed vs. signed up) and the current user's own status.
 */
public record ShiftSlotDto(
        String programId,
        String programName,
        String type,
        LocalDate date,
        String startTime,
        String endTime,
        String locationName,
        int trainersNeeded,
        int approvedCount,
        int pendingCount,
        String mySignupId,
        String myStatus,
        List<ShiftSignupTrainerDto> signups
) {
    public record ShiftSignupTrainerDto(String signupId, String trainerId, String trainerName, String status) {}
}
