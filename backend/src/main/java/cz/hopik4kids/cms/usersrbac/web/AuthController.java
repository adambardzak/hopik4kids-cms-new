package cz.hopik4kids.cms.usersrbac.web;

import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import cz.hopik4kids.cms.usersrbac.security.AppUserPrincipal;
import cz.hopik4kids.cms.usersrbac.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Admin authentication (prd §5.6, §7.1). Issues a JWT on successful login. */
@RestController
@RequestMapping("/admin/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserRepository users;

    public AuthController(AuthenticationManager authManager, JwtService jwt, UserRepository users) {
        this.authManager = authManager;
        this.jwt = jwt;
        this.users = users;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String token, String userId, String name, String role) {
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        try {
            var auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
            AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();

            User user = users.findById(principal.getId()).orElseThrow();
            user.setLastLoginAt(Instant.now());
            users.save(user);

            return new LoginResponse(jwt.issue(principal), principal.getId(),
                    user.getName(), principal.getRole());
        } catch (AuthenticationException e) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS", "Neplatné přihlašovací údaje");
        }
    }
}
