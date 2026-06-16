package com.linkvault.workspaces.entity;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.users.entity.User;
import com.linkvault.workspaces.enums.WorkspacePlan;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
    name = "workspaces",
    uniqueConstraints = @UniqueConstraint(name = "uk_workspaces_slug", columnNames = "slug"),
    indexes = {
        @Index(name = "idx_workspaces_owner", columnList = "owner_user_id"),
        @Index(name = "idx_workspaces_created_at", columnList = "created_at")
    }
)
@SQLDelete(sql = "UPDATE workspaces SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Workspace extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkspacePlan plan = WorkspacePlan.FREE;
}