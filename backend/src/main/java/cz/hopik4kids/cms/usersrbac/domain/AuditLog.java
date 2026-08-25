package cz.hopik4kids.cms.usersrbac.domain;

import cz.hopik4kids.cms.kernel.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Records who did what (prd §3B.7g, §7.3). Written on every write operation (create/update/delete)
 * for traceability. {@code at} == {@link BaseEntity#getCreatedAt()}.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

    /** Public id of the acting user; nullable for system/anonymous actions. */
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entity;

    @Column
    private String entityId;

    /** Arbitrary JSON detail (stored as text). */
    @Column(columnDefinition = "text")
    private String meta;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }
}
