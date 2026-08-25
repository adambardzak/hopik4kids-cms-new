package cz.hopik4kids.cms.usersrbac.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.usersrbac.service.UserService;
import cz.hopik4kids.cms.usersrbac.web.dto.InviteRequest;
import cz.hopik4kids.cms.usersrbac.web.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Team & role management (prd §7.3). Owner-only for role/lifecycle changes (prd §7.2). */
@RestController
@RequestMapping("/admin/api/users")
@PreAuthorize("hasRole('OWNER')")
public class AdminUserController {

    private final UserService service;

    public AdminUserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UserDto> list() {
        return PageResponse.ofAll(service.list());
    }

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void invite(@Valid @RequestBody InviteRequest req) {
        service.invite(req);
    }

    @PostMapping("/{id}/role")
    public UserDto changeRole(@PathVariable String id, @RequestParam String role) {
        return service.changeRole(id, role);
    }

    @PostMapping("/{id}/deactivate")
    public UserDto deactivate(@PathVariable String id) {
        return service.deactivate(id);
    }
}
