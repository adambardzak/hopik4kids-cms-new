package cz.hopik4kids.cms.scheduling.domain;

/**
 * State of a monthly work report. A month without a row is an (implicit) draft that the person
 * keeps correcting; SUBMITTED locks it for them, the owner then APPROVES or RETURNS it for fixes.
 */
public enum WorkReportStatus {
    SUBMITTED,
    APPROVED,
    RETURNED
}
