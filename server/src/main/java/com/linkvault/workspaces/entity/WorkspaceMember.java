package com.linkvault.workspaces.entity;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.users.entity.User;
import com.linkvault.workspaces.enums.WorkspaceRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
    name = "workspace_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_workspace_members_workspace_user",
        columnNames = {"workspace_id", "user_id"}
    ),
    indexes = {
        @Index(name = "idx_workspace_members_workspace", columnList = "workspace_id"),
        @Index(name = "idx_workspace_members_user", columnList = "user_id"),
        @Index(name = "idx_workspace_members_role", columnList = "role")
    }
)
public class WorkspaceMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspaceRole role;
}