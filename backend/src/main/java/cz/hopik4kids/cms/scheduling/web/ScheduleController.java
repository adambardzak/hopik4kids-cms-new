package cz.hopik4kids.cms.scheduling.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.scheduling.service.ScheduleService;
import cz.hopik4kids.cms.scheduling.web.dto.ScheduleEntryDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Weekly schedule (prd §6A.8 A). Occurrences generated from programs for a date range. */
@RestController
@RequestMapping("/admin/api/schedule")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ScheduleEntryDto> schedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String location) {
        List<ScheduleEntryDto> items = service.forRange(from, to, location);
        return PageResponse.ofAll(items);
    }
}
