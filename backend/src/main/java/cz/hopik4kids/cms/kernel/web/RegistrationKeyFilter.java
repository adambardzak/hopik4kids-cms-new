package cz.hopik4kids.cms.kernel.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Requires a shared secret header on the public registration write endpoint (prd §9).
 * Only the website's server (BFF) knows the key, so direct requests (curl/bots) are rejected
 * with 403. Read endpoints stay open (no sensitive data). Constant-time comparison.
 * <p>
 * If no key is configured (empty), the check is disabled — acceptable for local dev, but the
 * prod profile requires it (see application-prod.yml / SecretsGuard).
 */
@Component
public class RegistrationKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Registration-Key";

    private final byte[] expectedKey;
    private final boolean enabled;

    public RegistrationKeyFilter(@Value("${app.registration.api-key:}") String apiKey) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.expectedKey = enabled ? apiKey.getBytes(StandardCharsets.UTF_8) : new byte[0];
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !("/api/registrations".equals(uri) || "/api/waitlist".equals(uri));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided == null || !MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Nepovolený přístup\"}}");
            return;
        }
        chain.doFilter(request, response);
    }
}
