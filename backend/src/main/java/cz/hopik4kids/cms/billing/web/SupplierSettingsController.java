package cz.hopik4kids.cms.billing.web;

import cz.hopik4kids.cms.billing.domain.SupplierSettings;
import cz.hopik4kids.cms.billing.service.SupplierSettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Supplier (invoicing) settings — owner/admin (prd §6A.5). */
@RestController
@RequestMapping("/admin/api/billing/supplier")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
public class SupplierSettingsController {

    private final SupplierSettingsService service;

    public SupplierSettingsController(SupplierSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public SupplierSettings get() {
        return service.getOrDefault();
    }

    @PutMapping
    public SupplierSettings save(@RequestBody SupplierSettings body) {
        return service.save(body);
    }
}
