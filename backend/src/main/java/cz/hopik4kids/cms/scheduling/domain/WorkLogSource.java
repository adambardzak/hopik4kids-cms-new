package cz.hopik4kids.cms.scheduling.domain;

/** Where a work-log entry came from: entered by hand, or seeded from an approved shift. */
public enum WorkLogSource {
    MANUAL,
    SHIFT
}
