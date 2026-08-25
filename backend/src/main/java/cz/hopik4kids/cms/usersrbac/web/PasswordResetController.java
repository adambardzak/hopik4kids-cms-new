package cz.hopik4kids.cms.usersrbac.web;

import cz.hopik4kids.cms.usersrbac.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public password reset endpoints (prd §7.1). */
@RestController
@RequestMapping("/admin/auth")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    public record ResetRequest(@NotBlank @Email String email) {
    }

    public record ResetConfirm(
            @NotBlank String token,
            @NotBlank @Size(min = 8, message = "Heslo musí mít alespoň 8 znaků") String password) {
    }

    /** Always returns 202 (no account enumeration). */
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void request(@Valid @RequestBody ResetRequest req) {
        service.requestReset(req.email());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody ResetConfirm req) {
        service.confirmReset(req.token(), req.password());
    }
}
