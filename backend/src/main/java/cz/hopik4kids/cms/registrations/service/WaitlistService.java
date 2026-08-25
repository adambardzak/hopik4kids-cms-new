package cz.hopik4kids.cms.registrations.service;

import cz.hopik4kids.cms.core.domain.Program;
import cz.hopik4kids.cms.core.repository.ProgramRepository;
import cz.hopik4kids.cms.kernel.web.ApiException;
import cz.hopik4kids.cms.kernel.web.EnumParser;
import cz.hopik4kids.cms.registrations.domain.WaitlistEntry;
import cz.hopik4kids.cms.registrations.domain.WaitlistStatus;
import cz.hopik4kids.cms.registrations.repository.WaitlistEntryRepository;
import cz.hopik4kids.cms.registrations.web.dto.WaitlistEntryDto;
import cz.hopik4kids.cms.registrations.web.dto.WaitlistRequest;
import cz.hopik4kids.cms.usersrbac.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Waitlist for full programs (prd §6A.2). */
@Service
public class WaitlistService {

    private final WaitlistEntryRepository waitlist;
    private final ProgramRepository programs;
    private final AuditService audit;

    public WaitlistService(WaitlistEntryRepository waitlist,
                           ProgramRepository programs,
                           AuditService audit) {
        this.waitlist = waitlist;
        this.programs = programs;
        this.audit = audit;
    }

    /** Public: add to the waitlist. Only allowed when the program is actually full. */
    @Transactional
    public void add(WaitlistRequest req) {
        Program program = programs.findById(req.programId())
                .orElseThrow(() -> ApiException.notFound("Program nenalezen"));

        boolean full = program.getCapacity() != null && program.getSpotsTaken() >= program.getCapacity();
        if (!full) {
            throw ApiException.badRequest("NOT_FULL",
                    "Program není plný — použijte běžnou registraci");
        }

        WaitlistEntry e = new WaitlistEntry();
        e.setProgram(program);
        e.setChildName(req.childName());
        e.setParentName(req.parentName());
        e.setParentPhone(req.parentPhone().replaceAll("\\s+", ""));
        e.setParentEmail(req.parentEmail());
        e.setNote(blankToNull(req.note()));
        e.setStatus(WaitlistStatus.WAITING);
        waitlist.save(e);
        audit.record("waitlist-add", "Program", program.getId());
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryDto> list(String programId) {
        return waitlist.findByProgramIdOrderByCreatedAtAsc(programId).stream()
                .map(WaitlistEntryDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long waitingCount(String programId) {
        return waitlist.countByProgramIdAndStatus(programId, WaitlistStatus.WAITING);
    }

    @Transactional
    public WaitlistEntryDto setStatus(String id, String statusStr) {
        WaitlistStatus status = EnumParser.parseRequired(WaitlistStatus.class, statusStr, "status");
        WaitlistEntry e = waitlist.findById(id)
                .orElseThrow(() -> ApiException.notFound("Záznam nenalezen"));
        e.setStatus(status);
        waitlist.save(e);
        audit.record("waitlist-status", "WaitlistEntry", id, "{\"status\":\"" + status.name() + "\"}");
        return WaitlistEntryDto.from(e);
    }

    @Transactional
    public void delete(String id) {
        WaitlistEntry e = waitlist.findById(id)
                .orElseThrow(() -> ApiException.notFound("Záznam nenalezen"));
        waitlist.delete(e);
        audit.record("waitlist-delete", "WaitlistEntry", id);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
