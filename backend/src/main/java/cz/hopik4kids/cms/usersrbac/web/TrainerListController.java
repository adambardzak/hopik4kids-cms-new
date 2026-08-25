package cz.hopik4kids.cms.usersrbac.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.usersrbac.domain.Role;
import cz.hopik4kids.cms.usersrbac.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lightweight trainer list for program assignment (prd §7.5). Owner/admin. */
@RestController
@RequestMapping("/admin/api/trainers")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class TrainerListController {

    private final UserRepository users;

    public TrainerListController(UserRepository users) {
        this.users = users;
    }

    public record TrainerDto(String id, String name, String email) {
    }

    @GetMapping
    public PageResponse<TrainerDto> list() {
        var items = users.findByRoleOrderByName(Role.TRAINER).stream()
                .map(u -> new TrainerDto(u.getId(), u.getName(), u.getEmail()))
                .toList();
        return PageResponse.ofAll(items);
    }
}
