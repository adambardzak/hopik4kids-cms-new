package cz.hopik4kids.cms.billing.web;

import cz.hopik4kids.cms.billing.service.SchoolContractService;
import cz.hopik4kids.cms.billing.web.dto.InvoiceDto;
import cz.hopik4kids.cms.billing.web.dto.SchoolContractDto;
import cz.hopik4kids.cms.billing.web.dto.SchoolContractRequest;
import cz.hopik4kids.cms.billing.web.dto.SchoolInvoiceRequest;
import cz.hopik4kids.cms.core.web.dto.AdminProgramDto;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kindergarten morning programs + their invoicing ("Faktury školky", prd §6A.5). */
@RestController
@RequestMapping("/admin/api/billing/school-contracts")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
public class SchoolContractController {

    private final SchoolContractService service;

    public SchoolContractController(SchoolContractService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<SchoolContractDto> list() {
        return PageResponse.ofAll(service.list());
    }

    /** School programs that have no billing record yet (offered when adding a kindergarten). */
    @GetMapping("/candidates")
    public PageResponse<AdminProgramDto> candidates() {
        return PageResponse.ofAll(service.candidates());
    }

    @GetMapping("/{id}")
    public SchoolContractDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SchoolContractDto create(@RequestBody SchoolContractRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SchoolContractDto update(@PathVariable String id, @RequestBody SchoolContractRequest req) {
        return service.update(id, req);
    }

    /** Removes the billing record; the program (calendar, shifts, attendance) stays. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    /** Issue an invoice to the kindergarten for a period (defaults to the program's whole period). */
    @PostMapping("/{id}/invoice")
    public InvoiceDto invoice(@PathVariable String id, @RequestBody(required = false) SchoolInvoiceRequest req) {
        return service.issueInvoice(id, req == null
                ? new SchoolInvoiceRequest(null, null, null, null)
                : req);
    }
}
