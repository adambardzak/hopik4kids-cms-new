package cz.hopik4kids.cms.notifications.service;

import cz.hopik4kids.cms.notifications.domain.PushSubscription;
import cz.hopik4kids.cms.notifications.repository.PushSubscriptionRepository;
import cz.hopik4kids.cms.kernel.web.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages the current user's Web Push subscriptions. */
@Service
public class PushSubscriptionService {

    private final PushSubscriptionRepository subscriptions;

    public PushSubscriptionService(PushSubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    /** Registers (or refreshes) a push subscription for the current user. Idempotent per endpoint. */
    @Transactional
    public void subscribe(String endpoint, String p256dh, String auth) {
        String userId = SecurityUtils.currentUserId();
        PushSubscription sub = subscriptions.findByEndpoint(endpoint).orElseGet(PushSubscription::new);
        sub.setUserId(userId);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        subscriptions.save(sub);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        subscriptions.findByEndpoint(endpoint).ifPresent(subscriptions::delete);
    }
}
