package cz.hopik4kids.cms.usersrbac.security;

import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Spring Security principal wrapping the domain {@link User}. Role maps to authority {@code ROLE_<ROLE>}. */
public class AppUserPrincipal implements UserDetails {

    private final String id;
    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean active;

    public AppUserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().name();
        this.active = user.getStatus() == UserStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
