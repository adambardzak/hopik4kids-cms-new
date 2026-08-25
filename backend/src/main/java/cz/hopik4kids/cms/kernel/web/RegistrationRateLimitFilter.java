package cz.hopik4kids.cms.kernel.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
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
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limits the public registration POST (prd §5.3, §9.2) to prevent spam/abuse.
 * Simple in-memory token bucket keyed by client IP. For a multi-instance deployment this
 * should move to a shared store (e.g. Redis), but per-instance limiting already blunts abuse.
 */
@Component
public class RegistrationRateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final Duration refillPeriod;

    public RegistrationRateLimitFilter(
            @Value("${app.rate-limit.registration.capacity:5}") int capacity,
            @Value("${app.rate-limit.registration.refill-minutes:1}") long refillMinutes) {
        this.capacity = capacity;
        this.refillPeriod = Duration.ofMinutes(refillMinutes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/registrations".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        Bucket bucket = buckets.computeIfAbsent(clientIp(request), ip -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Příliš mnoho pokusů, zkuste to prosím za chvíli\"}}");
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod)
                        .build())
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
