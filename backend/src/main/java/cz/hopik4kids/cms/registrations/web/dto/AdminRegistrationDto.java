package cz.hopik4kids.cms.registrations.web.dto;

import cz.hopik4kids.cms.registrations.domain.Registration;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Admin registration view (prd §5.2b, §6.3) - full personal data including decrypted personalId (RČ).
 * Access restricted to owner/admin (prd §7.5). Never returned on the public API.
 */
public record AdminRegistrationDto(
        String id,
        String programId,
        String programName,
        String programType,
        String programLocationName,
        Integer programWeekday,
        String programTime,
        String programSchoolPart,
        // child
        String childName,
        LocalDate birthDate,
        String personalId,
        String childAddress,
        String healthInsurance,
        String className,
        // parent
        String parentName,
        String parentPhone,
        String parentEmail,
        String secondParentName,
        String secondParentPhone,
        // registration
        boolean wantsShirt,
        String shirtSize,
        String nickName,
        String allergies,
        String note,
        boolean consentPersonalData,
        boolean consentMedia,
        String paymentStatus,
        boolean overdue,
        String invoiceId,
        int priceSnapshot,
        String status,
        String source,
        Instant createdAt
) {
    public static AdminRegistrationDto from(Registration r) {
        return from(r, false, null);
    }

    /** Variant carrying invoice-derived info: overdue flag + invoice id (computed by the service). */
    public static AdminRegistrationDto from(Registration r, boolean overdue, String invoiceId) {
        var child = r.getChild();
        var parent = child.getParent();
        var program = r.getProgram();
        return new AdminRegistrationDto(
                r.getId(),
                program.getId(),
                program.getName(),
                program.getType().name().toLowerCase(),
                program.getLocation() == null ? null : program.getLocation().getName(),
                program.getWeekday(),
                program.getTime(),
                program.getSchoolPart() == null ? null : program.getSchoolPart().name().toLowerCase(),
                child.getFullName(),
                child.getBirthDate(),
                child.getPersonalId(),
                child.getAddress(),
                child.getHealthInsurance(),
                r.getClassName(),
                parent.getName(),
                parent.getPhone(),
                parent.getEmail(),
                parent.getSecondName(),
                parent.getSecondPhone(),
                r.isWantsShirt(),
                r.getShirtSize(),
                r.getNickName(),
                r.getAllergies(),
                r.getNote(),
                r.isConsentPersonalData(),
                r.isConsentMedia(),
                r.getPaymentStatus().name().toLowerCase(),
                overdue,
                invoiceId,
                r.getPriceSnapshot(),
                r.getStatus().name().toLowerCase(),
                r.getSource(),
                r.getCreatedAt()
        );
    }
}
