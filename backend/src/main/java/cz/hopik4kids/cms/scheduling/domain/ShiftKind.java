package cz.hopik4kids.cms.scheduling.domain;

/**
 * What a shift signup is for: a regular trainer shift (counts towards the lesson's trainersNeeded)
 * or the PR manager coming to film the lesson (informational for trainers, not capacity-limited).
 */
public enum ShiftKind {
    TRAINER,
    FILMING
}
