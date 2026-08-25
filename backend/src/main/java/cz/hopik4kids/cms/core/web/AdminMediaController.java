package cz.hopik4kids.cms.core.web;

import cz.hopik4kids.cms.core.service.MediaService;
import cz.hopik4kids.cms.core.web.dto.MediaDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Media upload (prd §5.6, §6.6 - article covers). Restricted to owner/admin. */
@RestController
@RequestMapping("/admin/api/media")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminMediaController {

    private final MediaService service;

    public AdminMediaController(MediaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaDto upload(@RequestParam("file") MultipartFile file,
                           @RequestParam(value = "alt", required = false) String alt) {
        return MediaDto.from(service.upload(file, alt));
    }
}
