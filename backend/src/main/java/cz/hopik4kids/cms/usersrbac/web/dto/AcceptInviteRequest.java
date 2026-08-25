package cz.hopik4kids.cms.usersrbac.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Accept an invitation by setting a name + password (prd §7.1). */
public record AcceptInviteRequest(
        @NotBlank String token,
        @NotBlank String name,
        @NotBlank @Size(min = 8, message = "Heslo musí mít alespoň 8 znaků") String password
) {
}
