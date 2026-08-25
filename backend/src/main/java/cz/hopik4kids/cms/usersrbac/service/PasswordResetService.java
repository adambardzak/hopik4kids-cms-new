package cz.hopik4kids.cms.usersrbac.service;

import cz.hopik4kids.cms.kernel.crypto.TokenGenerator;
import cz.hopik4kids.cms.kernel.email.EmailService;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.usersrbac.domain.PasswordResetToken;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.domain.UserStatus;
import cz.hopik4kids.cms.usersrbac.repository.PasswordResetTokenRepository;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Password reset flow (prd §7.1): request a reset link by email, then set a new password
 * with the one-time token. To avoid account enumeration, requesting a reset for an unknown
 * or non-active email succeeds silently (no email sent).
 */
@Service
public class PasswordResetService {

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final EmailService email;
    private final AuditService audit;
    private final long ttlHours;

    public PasswordResetService(UserRepository users,
                                PasswordResetTokenRepository tokens,
                                PasswordEncoder passwordEncoder,
                                EmailService email,
                                AuditService audit,
                                @Value("${app.password-reset.ttl-hours:2}") long ttlHours) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.audit = audit;
        this.ttlHours = ttlHours;
    }

    @Transactional
    public void requestReset(String emailAddress) {
        if (emailAddress == null || emailAddress.isBlank()) {
            return;
        }
        User user = users.findByEmail(emailAddress.toLowerCase()).orElse(null);
        // Silently succeed for unknown / inactive accounts (no enumeration).
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return;
        }

        String rawToken = TokenGenerator.randomToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(TokenGenerator.hash(rawToken));
        token.setExpiresAt(Instant.now().plus(ttlHours, ChronoUnit.HOURS));
        tokens.save(token);

        email.sendPasswordReset(user.getEmail(), rawToken);
        audit.record("password-reset-request", "User", user.getId());
    }

    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        PasswordResetToken token = tokens.findByTokenHash(TokenGenerator.hash(rawToken))
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "Neplatný odkaz pro obnovení hesla"));
        if (token.getUsedAt() != null) {
            throw ApiException.badRequest("ALREADY_USED", "Odkaz už byl použit");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("EXPIRED", "Odkaz vypršel");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw ApiException.badRequest("WEAK_PASSWORD", "Heslo musí mít alespoň 8 znaků");
        }

        User user = users.findById(token.getUserId())
                .orElseThrow(() -> ApiException.notFound("Uživatel nenalezen"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);

        token.setUsedAt(Instant.now());
        tokens.save(token);
        audit.record("password-reset-confirm", "User", user.getId());
    }
}
