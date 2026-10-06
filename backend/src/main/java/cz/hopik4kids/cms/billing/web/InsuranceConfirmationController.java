package cz.hopik4kids.cms.billing.web;

import cz.hopik4kids.cms.billing.service.InsuranceConfirmationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Backfill of payment confirmations that were never e-mailed. Owner/admin only. */
@RestController
@RequestMapping("/admin/api/billing/payment-confirmations")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class InsuranceConfirmationController {

    public record Result(int sent, int failed, int pending) {}

    private final InsuranceConfirmationService service;

    public InsuranceConfirmationController(InsuranceConfirmationService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public Result pending() {
        return new Result(0, 0, service.pendingRegistrationIds().size());
    }

    @PostMapping("/test")
    public Result test(@RequestParam String to) {
        boolean ok = service.sendTest(to);
        return new Result(ok ? 1 : 0, ok ? 0 : 1, service.pendingRegistrationIds().size());
    }

    @PostMapping("/send-pending")
    public Result sendPending() {
        int[] r = service.sendAllPending();
        return new Result(r[0], r[1], service.pendingRegistrationIds().size());
    }
}
