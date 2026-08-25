package cz.hopik4kids.cms.usersrbac.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Invite a team member (prd §7.3): email + role. */
public record InviteRequest(
        @NotBlank @Email String email,
        @NotBlank String role
) {
}
