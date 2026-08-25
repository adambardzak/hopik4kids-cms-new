package cz.hopik4kids.cms.usersrbac.web;

import cz.hopik4kids.cms.usersrbac.service.UserService;
import cz.hopik4kids.cms.usersrbac.web.dto.AcceptInviteRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public invitation acceptance (prd §7.1): the invitee sets their own name + password. */
@RestController
@RequestMapping("/admin/auth")
public class InvitationController {

    private final UserService service;

    public InvitationController(UserService service) {
        this.service = service;
    }

    @PostMapping("/accept-invite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@Valid @RequestBody AcceptInviteRequest req) {
        service.acceptInvitation(req);
    }
}
