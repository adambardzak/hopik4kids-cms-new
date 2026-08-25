package cz.hopik4kids.cms.core.web;

import cz.hopik4kids.cms.core.service.DashboardService;
import cz.hopik4kids.cms.core.web.dto.DashboardStatsDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard metrics (prd §5.6, §6A.1). */
@RestController
@RequestMapping("/admin/api/dashboard")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT','VIEWER')")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public DashboardStatsDto stats() {
        return service.stats();
    }
}
