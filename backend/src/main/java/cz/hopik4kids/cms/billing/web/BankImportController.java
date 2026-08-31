package cz.hopik4kids.cms.billing.web;

import cz.hopik4kids.cms.billing.service.BankImportService;
import cz.hopik4kids.cms.billing.web.dto.BankMatchDto;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Bank statement import + payment matching (prd todo #5). Owner/admin/accountant. */
@RestController
@RequestMapping("/admin/api/billing/bank-import")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
public class BankImportController {

    private final BankImportService service;

    public BankImportController(BankImportService service) {
        this.service = service;
    }

    /** Upload a statement CSV and get the proposed matches (no writes). */
    @PostMapping("/preview")
    public PageResponse<BankMatchDto> preview(@RequestParam("file") MultipartFile file) {
        return PageResponse.ofAll(service.preview(file));
    }

    /** Confirm: re-upload the same CSV + the tx ids to apply → marks matched invoices paid. */
    @PostMapping("/confirm")
    public BankImportService.ConfirmResult confirm(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "txIds", required = false) List<String> txIds) {
        return service.confirm(file, txIds);
    }
}
