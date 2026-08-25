package cz.hopik4kids.cms.registrations.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.registrations.service.WaitlistService;
import cz.hopik4kids.cms.registrations.web.dto.WaitlistEntryDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin waitlist management (prd §6A.2). Owner/admin. */
@RestController
@RequestMapping("/admin/api/waitlist")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminWaitlistController {

    private final WaitlistService service;

    public AdminWaitlistController(WaitlistService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<WaitlistEntryDto> list(@RequestParam String program) {
        return PageResponse.ofAll(service.list(program));
    }

    @PostMapping("/{id}/status")
    public WaitlistEntryDto setStatus(@PathVariable String id, @RequestParam String status) {
        return service.setStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
