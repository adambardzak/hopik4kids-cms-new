package cz.hopik4kids.cms.scheduling.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.scheduling.service.ShiftSignupService;
import cz.hopik4kids.cms.scheduling.web.dto.ShiftSlotDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Shift-signup (prd §7.4). Trainers self-serve; owner/admin approve. */
@RestController
@RequestMapping("/admin/api/shifts")
public class ShiftSignupController {

    private final ShiftSignupService service;

    public ShiftSignupController(ShiftSignupService service) {
        this.service = service;
    }

    /** All lesson occurrences in the range (with occupancy + my own status). */
    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    public PageResponse<ShiftSlotDto> open(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return PageResponse.ofAll(service.openSlots(from, to));
    }

    /** Only slots the current trainer is signed up for. */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    public PageResponse<ShiftSlotDto> mine(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return PageResponse.ofAll(service.mySlots(from, to));
    }

    public record SignupRequest(String programId, LocalDate date) {}

    @PostMapping("/signup")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void signup(@RequestBody SignupRequest req) {
        service.signup(req.programId(), req.date());
    }

    @DeleteMapping("/{signupId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String signupId) {
        service.cancel(signupId);
    }

    // --- admin approval ---

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public PageResponse<ShiftSlotDto.ShiftSignupTrainerDto> pending() {
        return PageResponse.ofAll(service.pending());
    }

    @PostMapping("/{signupId}/approve")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable String signupId) {
        service.decide(signupId, true);
    }

    @PostMapping("/{signupId}/reject")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable String signupId) {
        service.decide(signupId, false);
    }
}
