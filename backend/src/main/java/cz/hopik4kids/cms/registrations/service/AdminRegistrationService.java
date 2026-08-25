package cz.hopik4kids.cms.registrations.service;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.EnumParser;
import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.registrations.domain.RegistrationStatus;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.registrations.web.dto.AdminRegistrationDto;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminRegistrationService {

    private final RegistrationRepository registrations;
    private final ProgramRepository programs;
    private final AuditService audit;

    public AdminRegistrationService(RegistrationRepository registrations,
                                    ProgramRepository programs,
                                    AuditService audit) {
        this.registrations = registrations;
        this.programs = programs;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AdminRegistrationDto> list(String programId, String paymentStatus, String q) {
        PaymentStatus ps = EnumParser.parse(PaymentStatus.class, paymentStatus, "paymentStatus");
        return registrations.findForAdmin(blankToNull(programId), ps, blankToNull(q)).stream()
                .map(AdminRegistrationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminRegistrationDto get(String id) {
        return AdminRegistrationDto.from(find(id));
    }

    /** Soft-delete (prd §4.9): status=cancelled, decrement spotsTaken transactionally. */
    @Transactional
    public AdminRegistrationDto cancel(String id) {
        Registration r = find(id);
        if (r.getStatus() == RegistrationStatus.CANCELLED) {
            return AdminRegistrationDto.from(r);
        }
        r.setStatus(RegistrationStatus.CANCELLED);
        r.setPaymentStatus(PaymentStatus.CANCELLED);
        registrations.save(r);

        Program program = programs.findByIdForUpdate(r.getProgram().getId()).orElseThrow();
        program.setSpotsTaken(Math.max(0, program.getSpotsTaken() - 1));
        programs.save(program);

        audit.record("cancel", "Registration", id);
        return AdminRegistrationDto.from(r);
    }

    @Transactional
    public AdminRegistrationDto setPaymentStatus(String id, String status) {
        Registration r = find(id);
        PaymentStatus ps = EnumParser.parseRequired(PaymentStatus.class, status, "paymentStatus");
        r.setPaymentStatus(ps);
        registrations.save(r);
        audit.record("payment-status", "Registration", id, "{\"status\":\"" + ps.name() + "\"}");
        return AdminRegistrationDto.from(r);
    }

    private Registration find(String id) {
        return registrations.findById(id)
                .orElseThrow(() -> ApiException.notFound("Registrace nenalezena"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
