package cz.hopik4kids.cms.usersrbac.repository;

import cz.hopik4kids.cms.usersrbac.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
}
