package cz.hopik4kids.cms.notifications.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A Web Push subscription for a user's device/browser (PWA notifications).
 * One user may have several (phone, desktop). Identified uniquely by the push endpoint.
 */
@Entity
@Table(name = "push_subscription",
        uniqueConstraints = @UniqueConstraint(columnNames = "endpoint"))
public class PushSubscription extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 1000)
    private String endpoint;

    /** Client public key (p256dh) for message encryption. */
    @Column(name = "p256dh", nullable = false, length = 255)
    private String p256dh;

    /** Client auth secret for message encryption. */
    @Column(nullable = false, length = 255)
    private String auth;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public void setP256dh(String p256dh) {
        this.p256dh = p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }
}
