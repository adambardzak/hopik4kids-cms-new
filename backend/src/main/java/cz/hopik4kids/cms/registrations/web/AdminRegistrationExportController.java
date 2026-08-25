package cz.hopik4kids.cms.registrations.web;

import cz.hopik4kids.cms.registrations.service.RegistrationExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registration export CSV/XLSX (prd §5.6, §6.3). Owner/admin only (personal data). */
@RestController
@RequestMapping("/admin/api/registrations/export")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminRegistrationExportController {

    private final RegistrationExportService export;

    public AdminRegistrationExportController(RegistrationExportService export) {
        this.export = export;
    }

    @GetMapping
    public ResponseEntity<byte[]> exportRegistrations(
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(defaultValue = "csv") String format) {

        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        byte[] body = xlsx
                ? export.exportXlsx(program, paymentStatus)
                : export.exportCsv(program, paymentStatus);

        String filename = "registrace." + (xlsx ? "xlsx" : "csv");
        MediaType contentType = xlsx
                ? MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
