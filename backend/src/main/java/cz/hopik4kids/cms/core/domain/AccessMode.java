package cz.hopik4kids.cms.core.domain;

/** Access restriction for a program (prd §3B.10). */
public enum AccessMode {
    /** Normal public program, no checks. */
    PUBLIC,
    /** Listed with a restrictionNote, no hard check (default for kindergarten courses, MVP). */
    NOTICE_ONLY,
    /** Registration requires a valid access code (hash-verified server-side). */
    CODE,
    /** Not returned in public listing; reachable only via direct link. */
    UNLISTED
}
