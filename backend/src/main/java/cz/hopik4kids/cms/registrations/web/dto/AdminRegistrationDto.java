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
        int priceSnapshot,
        String status,
        String source,
        Instant createdAt
) {
    public static AdminRegistrationDto from(Registration r) {
        var child = r.getChild();
        var parent = child.getParent();
        var program = r.getProgram();
        return new AdminRegistrationDto(
                r.getId(),
                program.getId(),
                program.getName(),
                program.getType().name().toLowerCase(),
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
                r.getPriceSnapshot(),
                r.getStatus().name().toLowerCase(),
                r.getSource(),
                r.getCreatedAt()
        );
    }
}
