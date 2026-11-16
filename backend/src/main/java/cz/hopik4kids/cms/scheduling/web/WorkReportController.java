package cz.hopik4kids.cms.scheduling.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.scheduling.service.WorkReportService;
import cz.hopik4kids.cms.scheduling.web.dto.WorkReportDto;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Monthly work reports: person submits their month, owner/admin approves or returns it,
 * the accountant downloads approved reports (single PDF or the whole month as ZIP).
 */
@RestController
@RequestMapping("/admin/api/work-reports")
public class WorkReportController {

    private final WorkReportService service;

    public WorkReportController(WorkReportService service) {
        this.service = service;
    }

    /** My report for a month ('YYYY-MM'): status + live totals. */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER','PR')")
    public WorkReportDto mine(@RequestParam String period) {
        return service.mine(period);
    }

    @PostMapping("/mine/submit")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER','PR')")
    public WorkReportDto submit(@RequestParam String period) {
        return service.submit(period);
    }

    /** Everyone's reports for a month. */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
    public PageResponse<WorkReportDto> overview(@RequestParam String period) {
        return PageResponse.ofAll(service.overview(period));
    }

    @PostMapping("/{userId}/{period}/approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public WorkReportDto approve(@PathVariable String userId, @PathVariable String period) {
        return service.approve(userId, period);
    }

    public record ReturnRequest(String note) {}

    @PostMapping("/{userId}/{period}/return")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public WorkReportDto returnForFix(@PathVariable String userId, @PathVariable String period,
                                      @RequestBody(required = false) ReturnRequest req) {
        return service.returnForFix(userId, period, req == null ? null : req.note());
    }

    /** Single report as PDF (self, owner/admin, or accountant once approved). */
    @GetMapping("/{userId}/{period}/pdf")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER','PR','ACCOUNTANT')")
    public ResponseEntity<byte[]> pdf(@PathVariable String userId, @PathVariable String period) {
        return file(service.pdf(userId, period), "vykaz-" + period + ".pdf", MediaType.APPLICATION_PDF);
    }

    /** All approved reports of the month in one ZIP (PDFs + CSV summary). */
    @GetMapping("/approved.zip")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','ACCOUNTANT')")
    public ResponseEntity<byte[]> approvedZip(@RequestParam String period) {
        return file(service.approvedZip(period), "vykazy-" + period + ".zip",
                MediaType.parseMediaType("application/zip"));
    }

    private static ResponseEntity<byte[]> file(byte[] body, String filename, MediaType type) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
