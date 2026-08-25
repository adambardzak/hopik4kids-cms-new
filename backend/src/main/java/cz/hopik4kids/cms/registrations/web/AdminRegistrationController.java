package cz.hopik4kids.cms.registrations.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.registrations.service.AdminRegistrationService;
import cz.hopik4kids.cms.registrations.web.dto.AdminRegistrationDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin registration endpoints (prd §5.2b, §6.3). Personal data incl. RČ - owner/admin only (prd §7.5).
 * Trainer-scoped access (own programs only) is a later refinement (prd §7.5).
 */
@RestController
@RequestMapping("/admin/api/registrations")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminRegistrationController {

    private final AdminRegistrationService service;

    public AdminRegistrationController(AdminRegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdminRegistrationDto> list(
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String q) {
        return PageResponse.ofAll(service.list(program, paymentStatus, q));
    }

    @GetMapping("/{id}")
    public AdminRegistrationDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    public AdminRegistrationDto cancel(@PathVariable String id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/payment-status")
    public AdminRegistrationDto setPaymentStatus(@PathVariable String id,
                                                 @RequestParam String status) {
        return service.setPaymentStatus(id, status);
    }
}
