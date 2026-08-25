package cz.hopik4kids.cms.usersrbac.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/** Issues and validates JWT access tokens for admin auth (prd §7.1). */
@Component
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-seconds:86400}") long ttlSeconds) {
        byte[] decoded = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(decoded);
        this.ttlMillis = ttlSeconds * 1000;
    }

    public String issue(AppUserPrincipal principal) {
        Date now = new Date();
        return Jwts.builder()
                .subject(principal.getId())
                .claim("email", principal.getUsername())
                .claim("role", principal.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    /** Returns claims if the token is valid, else null. */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
