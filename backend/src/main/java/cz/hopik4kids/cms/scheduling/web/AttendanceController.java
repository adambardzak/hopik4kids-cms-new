package cz.hopik4kids.cms.scheduling.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.scheduling.service.AttendanceService;
import cz.hopik4kids.cms.scheduling.web.dto.AttendanceRowDto;
import cz.hopik4kids.cms.scheduling.web.dto.AttendanceSaveRequest;
import cz.hopik4kids.cms.scheduling.web.dto.AttendanceStatsDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Attendance recording + stats (prd §6A.3). Owner/admin/trainer. */
@RestController
@RequestMapping("/admin/api/attendance")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    /** Roster for a lesson (program + date): children + their recorded status. */
    @GetMapping
    public PageResponse<AttendanceRowDto> roster(
            @RequestParam String program,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return PageResponse.ofAll(service.roster(program, date));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(
            @RequestParam String program,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody AttendanceSaveRequest req) {
        service.save(program, date, req);
    }

    @GetMapping("/stats")
    public AttendanceStatsDto stats(@RequestParam String program) {
        return service.stats(program);
    }
}
