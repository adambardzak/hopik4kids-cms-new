package cz.hopik4kids.cms.registrations.service;

import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.email.EmailService;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.registrations.repository.RegistrationRepository;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bulk email to parents of a program's active participants (prd §6A.3): info before start,
 * schedule change, etc. Segmented — no manual address copying.
 */
@Service
public class BulkEmailService {

    private final RegistrationRepository registrations;
    private final ProgramRepository programs;
    private final EmailService email;
    private final AuditService audit;

    public BulkEmailService(RegistrationRepository registrations,
                            ProgramRepository programs,
                            EmailService email,
                            AuditService audit) {
        this.registrations = registrations;
        this.programs = programs;
        this.email = email;
        this.audit = audit;
    }

    /** Recipient emails (distinct parent emails of active registrations). */
    @Transactional(readOnly = true)
    public List<String> recipients(String programId) {
        programs.findById(programId).orElseThrow(() -> ApiException.notFound("Program nenalezen"));
        return registrations.findParentEmailsByProgram(programId);
    }

    public record SendResult(int total, int sent, int failed) {
    }

    @Transactional
    public SendResult send(String programId, String subject, String body) {
        if (subject == null || subject.isBlank() || body == null || body.isBlank()) {
            throw ApiException.badRequest("MISSING_CONTENT", "Předmět i text jsou povinné");
        }
        List<String> emails = recipients(programId);
        if (emails.isEmpty()) {
            throw ApiException.badRequest("NO_RECIPIENTS", "Program nemá žádné příjemce");
        }

        int sent = 0;
        int failed = 0;
        for (String to : emails) {
            boolean ok = email.send(to, subject, body);
            if (ok) sent++;
            else failed++;
        }
        audit.record("bulk-email", "Program", programId,
                "{\"total\":" + emails.size() + ",\"sent\":" + sent + ",\"failed\":" + failed + "}");
        return new SendResult(emails.size(), sent, failed);
    }
}
