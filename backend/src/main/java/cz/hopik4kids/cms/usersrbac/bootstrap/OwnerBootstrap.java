package cz.hopik4kids.cms.usersrbac.bootstrap;

import cz.hopik4kids.cms.usersrbac.domain.Role;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.domain.UserStatus;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the first owner if none exists (prd §7.2 - at least one owner must always exist).
 * In production the bootstrap credentials must be overridden via env.
 */
@Component
public class OwnerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String name;
    private final String password;

    public OwnerBootstrap(UserRepository users,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap.owner-email}") String email,
                          @Value("${app.bootstrap.owner-name}") String name,
                          @Value("${app.bootstrap.owner-password}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.name = name;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.OWNER)) {
            return;
        }
        User owner = new User();
        owner.setName(name);
        owner.setEmail(email);
        owner.setPasswordHash(passwordEncoder.encode(password));
        owner.setRole(Role.OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        users.save(owner);
        log.warn("Seeded bootstrap owner '{}'. Change the password immediately.", email);
    }
}
