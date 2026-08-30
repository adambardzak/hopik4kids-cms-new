package cz.hopik4kids.cms.core.web;

import cz.hopik4kids.cms.core.domain.Location;
import cz.hopik4kids.cms.core.domain.LocationKind;
import cz.hopik4kids.cms.core.repository.LocationRepository;
import cz.hopik4kids.cms.core.web.dto.AdminLocationDto;
import cz.hopik4kids.cms.core.web.dto.AdminLocationRequest;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.EnumParser;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
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

/** Admin Location CRUD (prd §5.6, §6.5). Restricted to owner/admin. */
@RestController
@RequestMapping("/admin/api/locations")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminLocationController {

    private final LocationRepository locations;
    private final AuditService audit;

    public AdminLocationController(LocationRepository locations, AuditService audit) {
        this.locations = locations;
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','TRAINER')")
    public PageResponse<AdminLocationDto> list() {
        return PageResponse.ofAll(locations.findAll().stream().map(AdminLocationDto::from).toList());
    }

    @GetMapping("/{id}")
    public AdminLocationDto get(@PathVariable String id) {
        return AdminLocationDto.from(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminLocationDto create(@RequestBody AdminLocationRequest req) {
        Location l = new Location();
        apply(l, req, true);
        l = locations.save(l);
        audit.record("create", "Location", l.getId());
        return AdminLocationDto.from(l);
    }

    @PutMapping("/{id}")
    public AdminLocationDto update(@PathVariable String id, @RequestBody AdminLocationRequest req) {
        Location l = find(id);
        apply(l, req, false);
        l = locations.save(l);
        audit.record("update", "Location", l.getId());
        return AdminLocationDto.from(l);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        Location l = find(id);
        locations.delete(l);
        audit.record("delete", "Location", id);
    }

    private Location find(String id) {
        return locations.findById(id).orElseThrow(() -> ApiException.notFound("Místo nenalezeno"));
    }

    private void apply(Location l, AdminLocationRequest req, boolean isCreate) {
        if (req.name() != null) {
            l.setName(req.name());
        }
        if (isCreate && (l.getName() == null || l.getName().isBlank())) {
            throw ApiException.badRequest("MISSING_NAME", "Název je povinný");
        }
        if (req.kind() != null) {
            l.setKind(EnumParser.parse(LocationKind.class, req.kind(), "kind"));
        } else if (isCreate) {
            throw ApiException.badRequest("MISSING_KIND", "Typ místa je povinný");
        }
        l.setAddress(req.address());
        l.setContactName(req.contactName());
        l.setContactPhone(req.contactPhone());
        l.setContactEmail(req.contactEmail());
        l.setNote(req.note());
    }
}
