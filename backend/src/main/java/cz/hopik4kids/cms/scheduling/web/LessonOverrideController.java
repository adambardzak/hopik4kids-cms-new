package cz.hopik4kids.cms.scheduling.web;

import cz.hopik4kids.cms.scheduling.service.LessonOverrideService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** Schedule overrides: cancel/move a recurring lesson or add a one-off (prd §7.4). Owner/admin only. */
@RestController
@RequestMapping("/admin/api/schedule/overrides")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class LessonOverrideController {

    private final LessonOverrideService service;

    public LessonOverrideController(LessonOverrideService service) {
        this.service = service;
    }

    public record CancelRequest(String programId, LocalDate originalDate, String note) {}

    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> cancel(@RequestBody CancelRequest req) {
        return Map.of("id", service.cancel(req.programId(), req.originalDate(), req.note()));
    }

    public record MoveRequest(String programId, LocalDate originalDate, LocalDate newDate, String newTime,
                              Integer durationMin, String locationId, String note) {}

    @PostMapping("/move")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> move(@RequestBody MoveRequest req) {
        return Map.of("id", service.move(req.programId(), req.originalDate(), req.newDate(), req.newTime(),
                req.durationMin(), req.locationId(), req.note()));
    }

    public record OneOffRequest(String programId, String title, LocalDate date, String time,
                                Integer durationMin, String locationId, String note) {}

    @PostMapping("/one-off")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> oneOff(@RequestBody OneOffRequest req) {
        return Map.of("id", service.oneOff(req.programId(), req.title(), req.date(), req.time(),
                req.durationMin(), req.locationId(), req.note()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
