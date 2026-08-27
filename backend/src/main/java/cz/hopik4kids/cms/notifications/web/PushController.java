package cz.hopik4kids.cms.notifications.web;

import cz.hopik4kids.cms.notifications.service.PushSubscriptionService;
import cz.hopik4kids.cms.notifications.service.WebPushService;
import cz.hopik4kids.cms.usersrbac.domain.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Web Push (PWA notifications): expose VAPID public key, manage subscriptions, test send. */
@RestController
@RequestMapping("/admin/api/push")
public class PushController {

    private final PushSubscriptionService subscriptions;
    private final WebPushService pushService;
    private final String publicKey;

    public PushController(PushSubscriptionService subscriptions, WebPushService pushService,
                          @Value("${app.push.vapid.public-key:}") String publicKey) {
        this.subscriptions = subscriptions;
        this.pushService = pushService;
        this.publicKey = publicKey;
    }

    /** VAPID public key the browser needs to create a subscription. Empty string = push disabled. */
    @GetMapping("/public-key")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> publicKey() {
        return Map.of("publicKey", publicKey == null ? "" : publicKey);
    }

    public record SubscribeRequest(String endpoint, String p256dh, String auth) {}

    @PostMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@RequestBody SubscribeRequest req) {
        subscriptions.subscribe(req.endpoint(), req.p256dh(), req.auth());
    }

    public record UnsubscribeRequest(String endpoint) {}

    @PostMapping("/unsubscribe")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@RequestBody UnsubscribeRequest req) {
        subscriptions.unsubscribe(req.endpoint());
    }

    /** Sends a test notification to the current privileged users (owner/admin). */
    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void test() {
        pushService.sendToRoles(List.of(Role.OWNER, Role.ADMIN),
                "Hopík4Kids", "Testovací notifikace — vše funguje ✅", "/admin");
    }
}
