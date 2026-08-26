package cz.hopik4kids.cms.scheduling.domain;

/** Kind of schedule override (prd §7.4 extension). */
public enum LessonOverrideType {
    /** A recurring lesson does not happen on this date (holiday, closure). */
    CANCELLED,
    /** A recurring lesson is moved to a different date/time. */
    MOVED,
    /** A one-off lesson/event not part of any recurring program. */
    ONE_OFF
}
