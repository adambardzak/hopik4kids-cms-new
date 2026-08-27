package cz.hopik4kids.cms.notifications.service;

import cz.hopik4kids.cms.notifications.domain.PushSubscription;
import cz.hopik4kids.cms.notifications.repository.PushSubscriptionRepository;
import cz.hopik4kids.cms.usersrbac.domain.Role;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.util.List;

/**
 * Sends Web Push notifications (PWA) to subscribed devices. Uses VAPID keys.
 * Dead subscriptions (410/404 from the push service) are pruned automatically.
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionRepository subscriptions;
    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private PushService pushService;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    public WebPushService(PushSubscriptionRepository subscriptions,
                          @Value("${app.push.vapid.public-key:}") String publicKey,
                          @Value("${app.push.vapid.private-key:}") String privateKey,
                          @Value("${app.push.vapid.subject:mailto:info@hopik4kids.cz}") String subject) {
        this.subscriptions = subscriptions;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    private synchronized PushService pushService() throws Exception {
        if (pushService == null) {
            pushService = new PushService(publicKey, privateKey, subject);
        }
        return pushService;
    }

    private boolean enabled() {
        return publicKey != null && !publicKey.isBlank() && privateKey != null && !privateKey.isBlank();
    }

    /** Sends a notification to every subscription of users holding any of the given roles. */
    @Transactional
    public void sendToRoles(List<Role> roles, String title, String body, String url) {
        if (!enabled()) {
            return;
        }
        List<PushSubscription> targets = subscriptions.findByUserRoles(roles);
        if (targets.isEmpty()) {
            return;
        }
        String payload = buildPayload(title, body, url);
        for (PushSubscription sub : targets) {
            send(sub, payload);
        }
    }

    private void send(PushSubscription sub, String payload) {
        try {
            Notification notification = new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
            var response = pushService().send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                // Subscription is gone — remove it.
                subscriptions.delete(sub);
            } else if (status >= 400) {
                log.warn("Push send returned {} for endpoint {}", status, truncate(sub.getEndpoint()));
            }
        } catch (Exception e) {
            log.warn("Push send failed for endpoint {}: {}", truncate(sub.getEndpoint()), e.getMessage());
        }
    }

    private String buildPayload(String title, String body, String url) {
        // Minimal JSON consumed by the service worker's push handler.
        return "{\"title\":\"" + esc(title) + "\",\"body\":\"" + esc(body)
                + "\",\"url\":\"" + esc(url) + "\"}";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String truncate(String s) {
        return s == null ? "" : s.substring(0, Math.min(40, s.length()));
    }
}
