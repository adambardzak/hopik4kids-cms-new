package cz.hopik4kids.cms.registrations.web.dto;

import java.util.List;

/**
 * Children of one program with only their photo/video consent (GDPR) — for the PR manager, who
 * must know whom they may film but must not see any other personal data.
 */
public record MediaConsentGroupDto(
        String programId,
        String programName,
        String locationName,
        String locationAddress,
        int consentCount,
        List<Child> children
) {
    public record Child(String name, boolean consentMedia) {}
}
