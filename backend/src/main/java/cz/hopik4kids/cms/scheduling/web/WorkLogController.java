package cz.hopik4kids.cms.scheduling.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.scheduling.service.WorkLogService;
import cz.hopik4kids.cms.scheduling.web.dto.WorkLogDto;
import cz.hopik4kids.cms.scheduling.web.dto.WorkLogRequest;
import cz.hopik4kids.cms.scheduling.web.dto.WorkLogSummaryDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Work logs / timesheets (prd todo #3). Trainers self-record; owner/admin approve + summarize. */
@RestController
@RequestMapping("/admin/api/work-logs")
public class WorkLogController {

    private final WorkLogService service;

    public WorkLogController(WorkLogService service) {
        this.service = service;
    }

    /** Trainer sees own entries; owner/admin see everyone's (optionally by date range). */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    public PageResponse<WorkLogDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return PageResponse.ofAll(service.list(from, to));
    }

    /** Per-trainer totals for a period (admin overview / payroll). */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public PageResponse<WorkLogSummaryDto> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return PageResponse.ofAll(service.summary(from, to));
    }

    /** Export the timesheet (entries + per-trainer totals) as XLSX/CSV for payroll. */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public org.springframework.http.ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "xlsx") String format) {
        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        byte[] body = xlsx ? service.exportXlsx(from, to) : service.exportCsv(from, to);
        String filename = "vykazy-hodin." + (xlsx ? "xlsx" : "csv");
        org.springframework.http.MediaType ct = xlsx
                ? org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8");
        return org.springframework.http.ResponseEntity.ok()
                .contentType(ct)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    public WorkLogDto create(@Valid @RequestBody WorkLogRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    public WorkLogDto update(@PathVariable String id, @Valid @RequestBody WorkLogRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    /** Seed pending entries from the current trainer's approved shifts in a period. */
    @PostMapping("/seed-from-shifts")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    public SeedResult seedFromShifts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return new SeedResult(service.seedFromShifts(from, to));
    }

    public record SeedResult(int created) {}

    // --- admin approval ---

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public WorkLogDto setStatus(@PathVariable String id, @RequestParam String status) {
        return service.setStatus(id, status);
    }
}
