package com.linkvault.tags.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
    name = "tags",
    uniqueConstraints = @UniqueConstraint(name = "uk_tags_workspace_name", columnNames = {"workspace_id", "name"}),
    indexes = {
        @Index(name = "idx_tags_user", columnList = "user_id"),
        @Index(name = "idx_tags_workspace", columnList = "workspace_id"),
        @Index(name = "idx_tags_created_at", columnList = "created_at")
    }
)
public class Tag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 40)
    private String color;
}