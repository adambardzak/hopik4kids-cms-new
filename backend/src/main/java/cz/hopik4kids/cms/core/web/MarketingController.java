package cz.hopik4kids.cms.core.web;

import cz.hopik4kids.cms.core.service.MarketingService;
import cz.hopik4kids.cms.core.web.dto.MarketingStatsDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Marketing & growth metrics (prd §6A.2). Owner/admin. */
@RestController
@RequestMapping("/admin/api/marketing")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class MarketingController {

    private final MarketingService service;

    public MarketingController(MarketingService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public MarketingStatsDto stats() {
        return service.stats();
    }
}
