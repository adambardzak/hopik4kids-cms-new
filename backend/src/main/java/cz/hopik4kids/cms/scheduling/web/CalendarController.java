package cz.hopik4kids.cms.scheduling.web;

import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.SecurityUtils;
import cz.hopik4kids.cms.scheduling.service.CalendarService;
import cz.hopik4kids.cms.usersrbac.domain.User;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/** Personal iCal calendar feed (webcal subscription) for schedule (prd §6A.8 A). */
@RestController
public class CalendarController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CalendarService calendar;
    private final UserRepository users;

    public CalendarController(CalendarService calendar, UserRepository users) {
        this.calendar = calendar;
        this.users = users;
    }

    /** Public iCal feed, authenticated by the unguessable token in the URL (calendar apps send no auth). */
    @GetMapping(value = "/api/calendar/{token}.ics", produces = "text/calendar; charset=utf-8")
    public ResponseEntity<String> feed(@PathVariable String token) {
        String ics = calendar.feedForToken(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/calendar; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"hopik4kids.ics\"")
                .body(ics);
    }

    /** Returns the current user's calendar token (generating one on first use). */
    @PostMapping("/admin/api/calendar/token")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public Map<String, String> token() {
        User user = users.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> ApiException.notFound("Uživatel nenalezen"));
        if (user.getCalendarToken() == null || user.getCalendarToken().isBlank()) {
            byte[] buf = new byte[24];
            RANDOM.nextBytes(buf);
            user.setCalendarToken(Base64.getUrlEncoder().withoutPadding().encodeToString(buf));
            users.save(user);
        }
        return Map.of("token", user.getCalendarToken());
    }
}
