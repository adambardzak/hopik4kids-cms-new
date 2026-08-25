package cz.hopik4kids.cms.kernel.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Helpers for the current authenticated principal (JWT subject = user id, authorities = ROLE_*). */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        String target = "ROLE_" + role;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (target.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /** True for owner/admin — full access, not trainer-scoped. */
    public static boolean isPrivileged() {
        return hasRole("OWNER") || hasRole("ADMIN");
    }
}
