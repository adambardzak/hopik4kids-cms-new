package cz.hopik4kids.cms.usersrbac.service;

import cz.hopik4kids.cms.usersrbac.domain.AuditLog;
import cz.hopik4kids.cms.usersrbac.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Writes audit entries on admin mutations (prd §3B.7g, §7.3). Actor = current JWT subject (user id). */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void record(String action, String entity, String entityId) {
        record(action, entity, entityId, null);
    }

    public void record(String action, String entity, String entityId, String meta) {
        AuditLog entry = new AuditLog();
        entry.setUserId(currentUserId());
        entry.setAction(action);
        entry.setEntity(entity);
        entry.setEntityId(entityId);
        entry.setMeta(meta);
        repo.save(entry);
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth == null || auth.getPrincipal() == null) ? null : auth.getName();
    }
}
