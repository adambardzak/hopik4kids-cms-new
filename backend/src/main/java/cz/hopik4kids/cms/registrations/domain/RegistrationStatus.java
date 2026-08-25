package cz.hopik4kids.cms.registrations.domain;

/** Registration lifecycle (prd §3B.5, §4.9). Soft-delete via CANCELLED. */
public enum RegistrationStatus {
    ACTIVE,
    CANCELLED
}
