package cz.hopik4kids.cms.registrations.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Public waitlist signup (prd §6A.2). Lightweight — no sensitive data. */
public record WaitlistRequest(
        @NotBlank(message = "Program je povinný")
        String programId,

        @NotBlank(message = "Jméno dítěte je povinné")
        String childName,

        @NotBlank(message = "Jméno rodiče je povinné")
        String parentName,

        @NotBlank(message = "Telefon je povinný")
        @Pattern(regexp = "^\\+420[0-9]{9}$", message = "Telefon musí být ve tvaru +420XXXXXXXXX")
        String parentPhone,

        @NotBlank(message = "E-mail je povinný")
        @Email(message = "Neplatný e-mail")
        String parentEmail,

        String note
) {
}
