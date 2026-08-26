package cz.hopik4kids.cms.core.web;

import cz.hopik4kids.cms.core.service.AdminProgramService;
import cz.hopik4kids.cms.core.web.dto.AdminProgramDto;
import cz.hopik4kids.cms.core.web.dto.AdminProgramRequest;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin Program CRUD (prd §5.6, §6.4). Reads allow trainers (scoped); writes are owner/admin (prd §7.2). */
@RestController
@RequestMapping("/admin/api/programs")
public class AdminProgramController {

    private final AdminProgramService service;

    public AdminProgramController(AdminProgramService service) {
        this.service = service;
    }

    /** Owner/admin: all programs. Trainer: only assigned programs (for attendance/schedule UI). */
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    public PageResponse<AdminProgramDto> list() {
        return PageResponse.ofAll(service.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AdminProgramDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProgramDto create(@RequestBody AdminProgramRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public AdminProgramDto update(@PathVariable String id, @RequestBody AdminProgramRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
