package cz.hopik4kids.cms.core.service;

import cz.hopik4kids.cms.core.domain.AccessMode;
import cz.hopik4kids.cms.core.domain.Location;
import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramStatus;
import cz.hopik4kids.cms.core.domain.ProgramType;
import cz.hopik4kids.cms.core.domain.SchoolPart;
import cz.hopik4kids.cms.core.domain.ShirtPolicy;
import cz.hopik4kids.cms.core.repository.LocationRepository;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.core.web.dto.AdminProgramDto;
import cz.hopik4kids.cms.core.web.dto.AdminProgramRequest;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.EnumParser;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminProgramService {

    private final ProgramRepository programs;
    private final LocationRepository locations;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public AdminProgramService(ProgramRepository programs,
                               LocationRepository locations,
                               PasswordEncoder passwordEncoder,
                               AuditService audit) {
        this.programs = programs;
        this.locations = locations;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AdminProgramDto> list() {
        return programs.findAll().stream().map(AdminProgramDto::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminProgramDto get(String id) {
        return AdminProgramDto.from(find(id));
    }

    @Transactional
    public AdminProgramDto create(AdminProgramRequest req) {
        Program p = new Program();
        apply(p, req, true);
        p = programs.save(p);
        audit.record("create", "Program", p.getId());
        return AdminProgramDto.from(p);
    }

    @Transactional
    public AdminProgramDto update(String id, AdminProgramRequest req) {
        Program p = find(id);
        apply(p, req, false);
        p = programs.save(p);
        audit.record("update", "Program", p.getId());
        return AdminProgramDto.from(p);
    }

    @Transactional
    public void delete(String id) {
        Program p = find(id);
        programs.delete(p);
        audit.record("delete", "Program", id);
    }

    private Program find(String id) {
        return programs.findById(id).orElseThrow(() -> ApiException.notFound("Program nenalezen"));
    }

    private void apply(Program p, AdminProgramRequest req, boolean isCreate) {
        if (isCreate) {
            p.setType(EnumParser.parseRequired(ProgramType.class, req.type(), "type"));
        } else if (req.type() != null) {
            p.setType(EnumParser.parse(ProgramType.class, req.type(), "type"));
        }
        if (req.name() != null) {
            p.setName(req.name());
        }
        if (isCreate && (p.getName() == null || p.getName().isBlank())) {
            throw ApiException.badRequest("MISSING_NAME", "Název je povinný");
        }
        p.setSlug(blankToNull(req.slug()));
        if (req.price() != null) {
            p.setPrice(req.price());
        }
        p.setCapacity(req.capacity());

        if (req.accessMode() != null) {
            p.setAccessMode(EnumParser.parse(AccessMode.class, req.accessMode(), "accessMode"));
        } else if (isCreate) {
            p.setAccessMode(AccessMode.PUBLIC);
        }
        p.setRestrictionNote(blankToNull(req.restrictionNote()));

        // Access code: hash if provided; require it when mode is CODE and none set yet.
        if (req.accessCode() != null && !req.accessCode().isBlank()) {
            p.setAccessCodeHash(passwordEncoder.encode(req.accessCode()));
        }
        if (p.getAccessMode() == AccessMode.CODE && p.getAccessCodeHash() == null) {
            throw ApiException.badRequest("ACCESS_CODE_REQUIRED",
                    "Přístupový kód je povinný pro režim 'code'");
        }

        if (req.shirtPolicy() != null) {
            p.setShirtPolicy(EnumParser.parse(ShirtPolicy.class, req.shirtPolicy(), "shirtPolicy"));
        } else if (isCreate) {
            p.setShirtPolicy(ShirtPolicy.NONE);
        }
        if (req.status() != null) {
            p.setStatus(EnumParser.parse(ProgramStatus.class, req.status(), "status"));
        } else if (isCreate) {
            p.setStatus(ProgramStatus.ACTIVE);
        }

        if (req.locationId() != null) {
            Location loc = locations.findById(req.locationId())
                    .orElseThrow(() -> ApiException.badRequest("INVALID_LOCATION", "Místo nenalezeno"));
            p.setLocation(loc);
        }

        p.setWeekday(req.weekday());
        p.setTime(blankToNull(req.time()));
        p.setSchoolPart(EnumParser.parse(SchoolPart.class, req.schoolPart(), "schoolPart"));
        p.setValidFrom(req.validFrom());
        p.setValidTo(req.validTo());
        p.setDurationMin(req.durationMin());
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
