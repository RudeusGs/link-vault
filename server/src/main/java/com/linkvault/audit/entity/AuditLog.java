package com.linkvault.audit.entity;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.users.entity.User;
import com.linkvault.workspaces.entity.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_workspace_created_at", columnList = "workspace_id, created_at"),
        @Index(name = "idx_audit_logs_actor", columnList = "actor_user_id")
    }
)
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(nullable = false, length = 80)
    private String targetType;

    private UUID targetId;

    @Column(columnDefinition = "text")
    private String metadata;
}