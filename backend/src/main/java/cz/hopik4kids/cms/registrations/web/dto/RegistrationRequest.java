package cz.hopik4kids.cms.registrations.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * Public registration payload (prd §5.3, §3B.5) - flat, unified for club & camp.
 * Server-side validation mirrors the website form (prd §4.6). Shirt/capacity/access-code
 * rules that depend on the program are enforced in the service layer.
 */
public record RegistrationRequest(

        @NotBlank(message = "Program je povinný")
        String programId,

        // --- child ---
        @NotBlank(message = "Jméno dítěte je povinné")
        String childName,

        @NotNull(message = "Datum narození je povinné")
        @PastOrPresent(message = "Datum narození nesmí být v budoucnosti")
        LocalDate birthDate,

        @NotBlank(message = "Rodné číslo je povinné")
        @Pattern(regexp = "^\\d{6}/\\d{4}$", message = "Rodné číslo musí být ve tvaru XXXXXX/XXXX")
        String personalId,

        @NotBlank(message = "Adresa je povinná")
        String address,

        @NotBlank(message = "Zdravotní pojišťovna je povinná")
        String healthInsurance,

        String className,

        // --- parent ---
        @NotBlank(message = "Jméno rodiče je povinné")
        String parentName,

        @NotBlank(message = "Telefon je povinný")
        @Pattern(regexp = "^\\+420[0-9]{9}$", message = "Telefon musí být ve tvaru +420XXXXXXXXX")
        String parentPhone,

        @NotBlank(message = "E-mail je povinný")
        @Email(message = "Neplatný e-mail")
        String parentEmail,

        String secondParentName,
        String secondParentPhone,

        // --- registration ---
        boolean wantsShirt,
        String shirtSize,
        String nickName,
        String allergies,
        String note,

        @AssertTrue(message = "Souhlas se zpracováním osobních údajů je povinný")
        boolean consentPersonalData,

        boolean consentMedia,

        String source,

        /** Required only when program.accessMode = CODE (prd §3B.10). */
        String accessCode
) {
}
