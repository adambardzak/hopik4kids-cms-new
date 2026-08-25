package cz.hopik4kids.cms.core.web;

import cz.hopik4kids.cms.core.repository.LocationRepository;
import cz.hopik4kids.cms.core.web.dto.PublicLocationDto;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public, read-only locations (prd §5.2, optional). */
@RestController
@RequestMapping("/api/locations")
public class PublicLocationController {

    private final LocationRepository locations;

    public PublicLocationController(LocationRepository locations) {
        this.locations = locations;
    }

    @GetMapping
    public PageResponse<PublicLocationDto> list() {
        List<PublicLocationDto> items = locations.findAll().stream()
                .map(PublicLocationDto::from)
                .toList();
        return PageResponse.ofAll(items);
    }
}
