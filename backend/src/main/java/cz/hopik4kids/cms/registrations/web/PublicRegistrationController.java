package cz.hopik4kids.cms.registrations.web;

import cz.hopik4kids.cms.registrations.service.RegistrationService;
import cz.hopik4kids.cms.registrations.web.dto.RegistrationRequest;
import cz.hopik4kids.cms.registrations.web.dto.RegistrationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single public registration write endpoint for clubs and camps (prd §5.3).
 * Validation is server-side (prd §4). Capacity + spotsTaken are handled transactionally
 * in {@link RegistrationService}.
 * <p>
 * TODO(prd §5.3, §9.2): add rate-limiting / anti-spam on this endpoint.
 */
@RestController
@RequestMapping("/api/registrations")
public class PublicRegistrationController {

    private final RegistrationService service;

    public PublicRegistrationController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest request) {
        return service.register(request);
    }
}
