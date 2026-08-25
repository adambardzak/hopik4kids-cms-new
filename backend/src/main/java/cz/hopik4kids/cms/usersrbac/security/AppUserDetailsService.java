package cz.hopik4kids.cms.usersrbac.security;

import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmail(email)
                .filter(u -> u.getPasswordHash() != null)
                .map(AppUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Uživatel nenalezen: " + email));
    }
}
