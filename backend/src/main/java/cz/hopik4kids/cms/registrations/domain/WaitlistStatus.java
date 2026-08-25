package cz.hopik4kids.cms.registrations.domain;

/** Waitlist entry status (prd §6A.2). */
public enum WaitlistStatus {
    /** Waiting for a spot. */
    WAITING,
    /** A spot opened and the parent was contacted / offered the place. */
    OFFERED,
    /** Converted into a real registration or otherwise resolved. */
    CONVERTED,
    /** No longer interested / removed. */
    CANCELLED
}
