package cz.hopik4kids.cms.usersrbac.service;

import cz.hopik4kids.cms.kernel.crypto.TokenGenerator;
import cz.hopik4kids.cms.kernel.email.EmailService;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.EnumParser;
import cz.hopik4kids.cms.usersrbac.domain.Invitation;
import cz.hopik4kids.cms.usersrbac.domain.Role;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.domain.UserStatus;
import cz.hopik4kids.cms.usersrbac.repository.InvitationRepository;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import cz.hopik4kids.cms.usersrbac.web.dto.AcceptInviteRequest;
import cz.hopik4kids.cms.usersrbac.web.dto.InviteRequest;
import cz.hopik4kids.cms.usersrbac.web.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Team & role management (prd §7.3). Onboarding is invite-based (prd §7.1): invite -> accept ->
 * set own password. At least one owner must always exist (prd §7.2, last-owner guard).
 */
@Service
public class UserService {

    private final UserRepository users;
    private final InvitationRepository invitations;
    private final PasswordEncoder passwordEncoder;
    private final EmailService email;
    private final AuditService audit;
    private final long invitationTtlHours;

    public UserService(UserRepository users,
                       InvitationRepository invitations,
                       PasswordEncoder passwordEncoder,
                       EmailService email,
                       AuditService audit,
                       @Value("${app.invitation.ttl-hours:72}") long invitationTtlHours) {
        this.users = users;
        this.invitations = invitations;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.audit = audit;
        this.invitationTtlHours = invitationTtlHours;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findAll().stream().map(UserDto::from).toList();
    }

    @Transactional
    public void invite(InviteRequest req) {
        Role role = EnumParser.parseRequired(Role.class, req.role(), "role");
        String emailLower = req.email().toLowerCase();

        users.findByEmail(emailLower).ifPresent(u -> {
            throw ApiException.conflict("EMAIL_TAKEN", "Uživatel s tímto e-mailem už existuje");
        });

        // Create an inactive user record so the member appears in the team list as "invited".
        User user = new User();
        user.setName(req.email());
        user.setEmail(emailLower);
        user.setRole(role);
        user.setStatus(UserStatus.INVITED);
        users.save(user);

        String rawToken = TokenGenerator.randomToken();
        Invitation inv = new Invitation();
        inv.setEmail(emailLower);
        inv.setRole(role);
        inv.setTokenHash(TokenGenerator.hash(rawToken));
        inv.setInvitedBy(currentUser());
        inv.setExpiresAt(Instant.now().plus(invitationTtlHours, ChronoUnit.HOURS));
        invitations.save(inv);

        String inviterName = currentUser() != null ? currentUser().getName() : "Hopík4Kids";
        email.sendInvitation(emailLower, inviterName, rawToken);
        audit.record("invite", "User", user.getId(), "{\"role\":\"" + role.name() + "\"}");
    }

    @Transactional
    public void acceptInvitation(AcceptInviteRequest req) {
        Invitation inv = invitations.findByTokenHash(TokenGenerator.hash(req.token()))
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "Neplatná pozvánka"));
        if (inv.getAcceptedAt() != null) {
            throw ApiException.badRequest("ALREADY_ACCEPTED", "Pozvánka už byla použita");
        }
        if (inv.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("EXPIRED", "Pozvánka vypršela");
        }

        User user = users.findByEmail(inv.getEmail())
                .orElseThrow(() -> ApiException.notFound("Uživatel nenalezen"));
        user.setName(req.name());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setStatus(UserStatus.ACTIVE);
        users.save(user);

        inv.setAcceptedAt(Instant.now());
        invitations.save(inv);
        audit.record("accept-invite", "User", user.getId());
    }

    @Transactional
    public UserDto changeRole(String userId, String roleStr) {
        Role role = EnumParser.parseRequired(Role.class, roleStr, "role");
        User user = find(userId);
        // Guard: do not demote the last owner (prd §7.2).
        if (user.getRole() == Role.OWNER && role != Role.OWNER) {
            ensureNotLastOwner(user);
        }
        user.setRole(role);
        users.save(user);
        audit.record("change-role", "User", userId, "{\"role\":\"" + role.name() + "\"}");
        return UserDto.from(user);
    }

    @Transactional
    public UserDto deactivate(String userId) {
        User user = find(userId);
        if (user.getRole() == Role.OWNER) {
            ensureNotLastOwner(user);
        }
        user.setStatus(UserStatus.DISABLED);
        users.save(user);
        audit.record("deactivate", "User", userId);
        return UserDto.from(user);
    }

    private void ensureNotLastOwner(User owner) {
        long activeOwners = users.findAll().stream()
                .filter(u -> u.getRole() == Role.OWNER && u.getStatus() == UserStatus.ACTIVE)
                .count();
        if (activeOwners <= 1) {
            throw ApiException.badRequest("LAST_OWNER", "Musí zůstat alespoň jeden vlastník");
        }
    }

    private User find(String id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Uživatel nenalezen"));
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return users.findById(auth.getName()).orElse(null);
    }
}
