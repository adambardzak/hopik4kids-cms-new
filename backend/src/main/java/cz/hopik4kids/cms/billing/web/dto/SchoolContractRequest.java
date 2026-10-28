package cz.hopik4kids.cms.billing.web.dto;

import java.time.LocalDate;

/**
 * Create/update payload for a kindergarten billing record. Lesson fields are written through to
 * the linked Program (that is what reaches the calendar); the rest is stored on the contract.
 * <p>
 * {@code programId} on create links an already existing school program instead of making a new one.
 * Null lesson fields on update mean "leave unchanged".
 */
public record SchoolContractRequest(
        String programId,
        String schoolName,
        String locationId,
        Integer weekday,
        String time,
        Integer durationMin,
        LocalDate periodFrom,
        LocalDate periodTo,
        Integer trainersNeeded,
        String billingName,
        String billingEmail,
        String billingAddress,
        String ico,
        String dic,
        Integer price,
        String priceUnit,
        String note
) {
}
