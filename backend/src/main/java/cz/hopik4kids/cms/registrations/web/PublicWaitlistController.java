package cz.hopik4kids.cms.registrations.web;

import cz.hopik4kids.cms.registrations.service.WaitlistService;
import cz.hopik4kids.cms.registrations.web.dto.WaitlistRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public waitlist signup for full programs (prd §6A.2). Protected by the shared registration key. */
@RestController
@RequestMapping("/api/waitlist")
public class PublicWaitlistController {

    private final WaitlistService service;

    public PublicWaitlistController(WaitlistService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@Valid @RequestBody WaitlistRequest request) {
        service.add(request);
    }
}
