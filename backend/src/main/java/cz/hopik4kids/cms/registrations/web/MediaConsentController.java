package cz.hopik4kids.cms.registrations.web;

import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.registrations.service.MediaConsentService;
import cz.hopik4kids.cms.registrations.web.dto.MediaConsentGroupDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Children lists with only the photo/video consent flag (PR manager + admins). */
@RestController
@RequestMapping("/admin/api/media-consents")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','PR')")
public class MediaConsentController {

    private final MediaConsentService service;

    public MediaConsentController(MediaConsentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<MediaConsentGroupDto> list() {
        return PageResponse.ofAll(service.list());
    }
}
