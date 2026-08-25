package cz.hopik4kids.cms.usersrbac.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Password reset token (prd §7.1). Sent by email as a one-time link; stored hashed.
 * Single-use (marked used) and time-limited.
 */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken extends BaseEntity {

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant usedAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
