package com.linkvault.vaults.entity;

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

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
    name = "vaults",
    uniqueConstraints = @UniqueConstraint(name = "uk_vaults_workspace_name", columnNames = {"workspace_id", "name"}),
    indexes = {
        @Index(name = "idx_vaults_user", columnList = "user_id"),
        @Index(name = "idx_vaults_workspace", columnList = "workspace_id"),
        @Index(name = "idx_vaults_created_at", columnList = "created_at")
    }
)
@SQLDelete(sql = "UPDATE vaults SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Vault extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 80)
    private String icon;

    @Column(length = 40)
    private String color;
}