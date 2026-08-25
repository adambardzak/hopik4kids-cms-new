package cz.hopik4kids.cms.registrations.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.registrations.service.BulkEmailService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bulk email to a program's participants (prd §6A.3). Owner/admin only. */
@RestController
@RequestMapping("/admin/api/bulk-email")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class BulkEmailController {

    private final BulkEmailService service;

    public BulkEmailController(BulkEmailService service) {
        this.service = service;
    }

    /** Preview recipients (distinct parent emails) for a program. */
    @GetMapping("/recipients")
    public PageResponse<String> recipients(@RequestParam String program) {
        return PageResponse.ofAll(service.recipients(program));
    }

    public record SendRequest(@NotBlank String program, @NotBlank String subject, @NotBlank String body) {
    }

    @PostMapping
    public BulkEmailService.SendResult send(@RequestBody SendRequest req) {
        return service.send(req.program(), req.subject(), req.body());
    }
}
