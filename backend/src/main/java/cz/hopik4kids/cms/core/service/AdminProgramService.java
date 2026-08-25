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
    private final cz.hopik4kids.cms.registrations.repository.RegistrationRepository registrations;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public AdminProgramService(ProgramRepository programs,
                               LocationRepository locations,
                               cz.hopik4kids.cms.registrations.repository.RegistrationRepository registrations,
                               PasswordEncoder passwordEncoder,
                               AuditService audit) {
        this.programs = programs;
        this.locations = locations;
        this.registrations = registrations;
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
        long regs = registrations.countByProgramId(id);
        if (regs > 0) {
            throw ApiException.conflict("PROGRAM_HAS_REGISTRATIONS",
                    "Program nelze smazat, má " + regs + " registrací. Nejdřív je zrušte, nebo program archivujte.");
        }
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

        // Consistency checks.
        if (p.getPrice() < 0) {
            throw ApiException.badRequest("INVALID_PRICE", "Cena nesmí být záporná");
        }
        if (p.getCapacity() != null) {
            if (p.getCapacity() < 0) {
                throw ApiException.badRequest("INVALID_CAPACITY", "Kapacita nesmí být záporná");
            }
            if (p.getCapacity() < p.getSpotsTaken()) {
                throw ApiException.badRequest("CAPACITY_BELOW_TAKEN",
                        "Kapacitu nelze snížit pod počet přihlášených dětí (" + p.getSpotsTaken() + ")");
            }
        }
        if (p.getWeekday() != null && (p.getWeekday() < 1 || p.getWeekday() > 7)) {
            throw ApiException.badRequest("INVALID_WEEKDAY", "Den v týdnu musí být 1–7");
        }
        if (p.getTime() != null && !p.getTime().matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            throw ApiException.badRequest("INVALID_TIME", "Čas musí být ve tvaru HH:MM");
        }
        if (p.getValidFrom() != null && p.getValidTo() != null && p.getValidFrom().isAfter(p.getValidTo())) {
            throw ApiException.badRequest("INVALID_PERIOD", "Období 'od' nesmí být po 'do'");
        }
        if (p.getStartDate() != null && p.getEndDate() != null && p.getStartDate().isAfter(p.getEndDate())) {
            throw ApiException.badRequest("INVALID_DATES", "Datum 'od' nesmí být po 'do'");
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
