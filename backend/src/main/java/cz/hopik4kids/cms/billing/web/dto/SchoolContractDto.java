package cz.hopik4kids.cms.billing.web.dto;

import java.time.LocalDate;

/**
 * A kindergarten morning program as seen on the "Faktury školky" page: the lesson facts come from
 * the linked Program (so they stay in sync with the calendar), the billing facts from the contract.
 */
public record SchoolContractDto(
        String id,
        String programId,
        /** Kindergarten / program name. */
        String schoolName,
        String locationId,
        String locationName,
        String locationAddress,
        Integer weekday,
        String time,
        Integer durationMin,
        LocalDate periodFrom,
        LocalDate periodTo,
        int trainersNeeded,
        String programStatus,
        String billingName,
        String billingEmail,
        String billingAddress,
        String ico,
        String dic,
        int price,
        String priceUnit,
        String note,
        /** Lessons the program has in its whole period (cancelled ones excluded). */
        int lessonsInPeriod,
        /** Amount a single invoice for the whole period would come to. */
        int periodAmount
) {
}
