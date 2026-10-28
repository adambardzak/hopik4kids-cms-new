package cz.hopik4kids.cms.billing.web.dto;

import java.time.LocalDate;

/**
 * Issue an invoice to a kindergarten. Period defaults to the program's whole period; lesson count
 * is derived from the calendar and the total from the agreed price — both can be overridden by hand
 * (e.g. when a lesson was swapped or a different amount was agreed).
 */
public record SchoolInvoiceRequest(
        LocalDate periodFrom,
        LocalDate periodTo,
        Integer lessonCount,
        Integer totalAmount
) {
}
