package cz.hopik4kids.cms.billing.domain;

/** How the price agreed with a kindergarten is expressed (invoice math follows from it). */
public enum SchoolPriceUnit {
    /** Price per taught lesson — invoice = lessons in the period × price. */
    LESSON,
    /** Price per month — invoice = months with at least one lesson × price. */
    MONTH,
    /** Flat price for the whole period. */
    PERIOD
}
