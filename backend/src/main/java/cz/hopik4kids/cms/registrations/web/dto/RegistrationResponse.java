package cz.hopik4kids.cms.registrations.web.dto;

/** Public registration confirmation (prd §5.3). No personal data echoed back. */
public record RegistrationResponse(String id, String programId, int priceSnapshot, String status) {
}
