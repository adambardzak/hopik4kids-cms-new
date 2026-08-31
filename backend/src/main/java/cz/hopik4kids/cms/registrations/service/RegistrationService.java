package cz.hopik4kids.cms.registrations.service;

import cz.hopik4kids.cms.core.domain.AccessMode;
import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.domain.ProgramStatus;
import cz.hopik4kids.cms.core.domain.ShirtPolicy;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.registrations.domain.Child;
import cz.hopik4kids.cms.registrations.domain.Parent;
import cz.hopik4kids.cms.registrations.domain.PaymentStatus;
import cz.hopik4kids.cms.registrations.domain.Registration;
import cz.hopik4kids.cms.registrations.domain.RegistrationStatus;
import cz.hopik4kids.cms.registrations.repository.ChildRepository;
import cz.hopik4kids.cms.registrations.repository.ParentRepository;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.registrations.web.dto.RegistrationRequest;
import cz.hopik4kids.cms.registrations.web.dto.RegistrationResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration creation (prd §4, §5.3). Enforces business rules server-side:
 * shirt policy, access mode/code, and the transactional capacity check + spotsTaken increment
 * (prd §3B.9, §4.2) - no overbooking under concurrency (row is pessimistically locked).
 */
@Service
public class RegistrationService {

    /** Shirt surcharge in CZK (prd §4.5). */
    static final int SHIRT_PRICE = 500;

    private final ProgramRepository programs;
    private final ParentRepository parents;
    private final ChildRepository children;
    private final RegistrationRepository registrations;
    private final PasswordEncoder passwordEncoder;
    private final cz.hopik4kids.cms.notifications.service.WebPushService webPush;
    // Lazy to avoid a bean cycle (billing depends on registrations too).
    private final org.springframework.beans.factory.ObjectProvider<cz.hopik4kids.cms.billing.service.InvoiceService> invoiceService;
    private final org.springframework.beans.factory.ObjectProvider<cz.hopik4kids.cms.billing.service.InvoiceEmailService> invoiceEmailService;

    public RegistrationService(ProgramRepository programs,
                               ParentRepository parents,
                               ChildRepository children,
                               RegistrationRepository registrations,
                               PasswordEncoder passwordEncoder,
                               cz.hopik4kids.cms.notifications.service.WebPushService webPush,
                               org.springframework.beans.factory.ObjectProvider<cz.hopik4kids.cms.billing.service.InvoiceService> invoiceService,
                               org.springframework.beans.factory.ObjectProvider<cz.hopik4kids.cms.billing.service.InvoiceEmailService> invoiceEmailService) {
        this.programs = programs;
        this.parents = parents;
        this.children = children;
        this.registrations = registrations;
        this.passwordEncoder = passwordEncoder;
        this.webPush = webPush;
        this.invoiceService = invoiceService;
        this.invoiceEmailService = invoiceEmailService;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest req) {
        // Lock the program row for the capacity check + counter mutation (prd §4.2).
        Program program = programs.findByIdForUpdate(req.programId())
                .orElseThrow(() -> ApiException.notFound("Program nenalezen"));

        if (program.getStatus() != ProgramStatus.ACTIVE) {
            throw ApiException.badRequest("PROGRAM_NOT_ACTIVE", "Program není otevřen k registraci");
        }

        verifyAccess(program, req.accessCode());
        boolean wantsShirt = resolveShirt(program, req);
        verifyCapacity(program);

        // Persist personal data (variant A: Parent + Child).
        Parent parent = new Parent();
        parent.setName(req.parentName());
        parent.setPhone(normalizePhone(req.parentPhone()));
        parent.setEmail(req.parentEmail());
        parent.setSecondName(blankToNull(req.secondParentName()));
        parent.setSecondPhone(blankToNull(req.secondParentPhone()));
        parent = parents.save(parent);

        Child child = new Child();
        child.setFullName(req.childName());
        child.setBirthDate(req.birthDate());
        child.setPersonalId(req.personalId());
        child.setAddress(req.address());
        child.setHealthInsurance(req.healthInsurance());
        child.setParent(parent);
        child = children.save(child);

        int priceSnapshot = program.getPrice() + (wantsShirt ? SHIRT_PRICE : 0);

        Registration reg = new Registration();
        reg.setProgram(program);
        reg.setChild(child);
        reg.setClassName(blankToNull(req.className()));
        reg.setWantsShirt(wantsShirt);
        reg.setShirtSize(wantsShirt ? req.shirtSize() : null);
        reg.setNickName(blankToNull(req.nickName()));
        reg.setAllergies(blankToNull(req.allergies()));
        reg.setNote(blankToNull(req.note()));
        reg.setConsentPersonalData(req.consentPersonalData());
        reg.setConsentMedia(req.consentMedia());
        reg.setPaymentStatus(PaymentStatus.UNPAID);
        reg.setPriceSnapshot(priceSnapshot);
        reg.setStatus(RegistrationStatus.ACTIVE);
        reg.setSource(blankToNull(req.source()));
        reg = registrations.save(reg);

        // Transactional counter bump (prd §3B.9).
        program.setSpotsTaken(program.getSpotsTaken() + 1);
        programs.save(program);

        // Notify owners/admins on their phones (PWA push). Best-effort — never fails the registration.
        try {
            webPush.sendToRoles(
                    java.util.List.of(cz.hopik4kids.cms.usersrbac.domain.Role.OWNER,
                            cz.hopik4kids.cms.usersrbac.domain.Role.ADMIN),
                    "Nová registrace",
                    child.getFullName() + " — " + program.getName(),
                    "/admin/registrace?program=" + program.getId());
        } catch (Exception ignored) {
            // push is non-critical
        }

        // Auto-invoice: when a parent registers for a paid club/camp, generate the invoice and
        // email it with a thank-you note. Schools are invoiced manually by admins (school pays).
        // Best-effort — a failure here must never fail the registration itself.
        maybeAutoInvoice(reg, program, parent);

        return new RegistrationResponse(reg.getId(), program.getId(), priceSnapshot,
                reg.getStatus().name().toLowerCase());
    }

    private void maybeAutoInvoice(Registration reg, Program program, Parent parent) {
        boolean parentPays = program.getType() == cz.hopik4kids.cms.core.domain.ProgramType.CLUB
                || program.getType() == cz.hopik4kids.cms.core.domain.ProgramType.CAMP;
        boolean hasEmail = parent.getEmail() != null && !parent.getEmail().isBlank();
        if (!parentPays || reg.getPriceSnapshot() <= 0 || !hasEmail) {
            return;
        }
        try {
            var invoice = invoiceService.getObject().createFromRegistration(reg.getId());
            invoiceEmailService.getObject().sendWelcome(
                    invoice.id(), reg.getChild().getFullName(), program.getName());
        } catch (Exception e) {
            // Auto-invoicing is best-effort; admins can always issue/send the invoice manually.
            org.slf4j.LoggerFactory.getLogger(RegistrationService.class)
                    .warn("Auto-invoice failed for registration {}: {}", reg.getId(), e.getMessage());
        }
    }

    private void verifyAccess(Program program, String accessCode) {
        if (program.getAccessMode() == AccessMode.CODE) {
            if (accessCode == null || accessCode.isBlank()
                    || program.getAccessCodeHash() == null
                    || !passwordEncoder.matches(accessCode, program.getAccessCodeHash())) {
                throw ApiException.forbidden("INVALID_ACCESS_CODE", "Neplatný přístupový kód");
            }
        }
    }

    private boolean resolveShirt(Program program, RegistrationRequest req) {
        boolean wantsShirt = switch (program.getShirtPolicy()) {
            case NONE -> false;
            case REQUIRED -> true;
            case OPTIONAL -> req.wantsShirt();
        };
        if (wantsShirt && (req.shirtSize() == null || req.shirtSize().isBlank())) {
            throw ApiException.badRequest("SHIRT_SIZE_REQUIRED", "Velikost dresu je povinná");
        }
        return wantsShirt;
    }

    private void verifyCapacity(Program program) {
        Integer capacity = program.getCapacity();
        if (capacity != null && program.getSpotsTaken() >= capacity) {
            throw ApiException.conflict("PROGRAM_FULL", "Program je plný");
        }
    }

    private static String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("\\s+", "");
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
