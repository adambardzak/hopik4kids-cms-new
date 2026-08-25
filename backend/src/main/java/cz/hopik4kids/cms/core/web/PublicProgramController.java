package cz.hopik4kids.cms.core.web;

import cz.hopik4kids.cms.core.domain.AccessMode;
import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramStatus;
import cz.hopik4kids.cms.core.domain.ProgramType;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.core.web.dto.PublicProgramDto;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/** Public, read-only program endpoints (prd §5.2). No personal data; capacity comes from spotsTaken. */
@RestController
@RequestMapping("/api/programs")
public class PublicProgramController {

    private final ProgramRepository programs;

    public PublicProgramController(ProgramRepository programs) {
        this.programs = programs;
    }

    /**
     * Listing. Defaults to status=active. UNLISTED programs are never returned here (prd §4.10) -
     * they are reachable only via GET /:id.
     */
    @GetMapping
    public PageResponse<PublicProgramDto> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "active") String status,
            @RequestParam(required = false) String include) {

        ProgramStatus programStatus = parseStatus(status);
        boolean includeLocation = "location".equalsIgnoreCase(include);

        List<Program> found;
        if (type == null || type.isBlank()) {
            found = includeLocation
                    ? programs.findByStatusWithLocation(programStatus)
                    : programs.findByStatus(programStatus);
        } else {
            ProgramType t = parseType(type);
            found = includeLocation
                    ? programs.findByTypeAndStatusWithLocation(t, programStatus)
                    : programs.findByTypeAndStatus(t, programStatus);
        }

        List<PublicProgramDto> items = found.stream()
                .filter(p -> p.getAccessMode() != AccessMode.UNLISTED)
                .map(p -> PublicProgramDto.from(p, includeLocation))
                .toList();

        return PageResponse.ofAll(items);
    }

    /** Detail by public id. Returns UNLISTED programs too (direct link is the access mechanism). */
    @GetMapping("/{id}")
    public PublicProgramDto get(@PathVariable String id,
                                @RequestParam(required = false) String include) {
        boolean includeLocation = "location".equalsIgnoreCase(include);
        Program p = (includeLocation
                ? programs.findByIdWithLocation(id)
                : programs.findById(id))
                .orElseThrow(() -> ApiException.notFound("Program nenalezen"));
        return PublicProgramDto.from(p, includeLocation);
    }

    private ProgramType parseType(String type) {
        try {
            return ProgramType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_TYPE", "Neplatný typ programu: " + type);
        }
    }

    private ProgramStatus parseStatus(String status) {
        try {
            return ProgramStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_STATUS", "Neplatný stav: " + status);
        }
    }
}
