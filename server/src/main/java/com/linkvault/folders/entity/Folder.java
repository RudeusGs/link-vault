package com.linkvault.folders.entity;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.vaults.entity.Vault;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    name = "folders",
    indexes = {
        @Index(name = "idx_folders_vault", columnList = "vault_id"),
        @Index(name = "idx_folders_parent", columnList = "parent_id"),
        @Index(name = "idx_folders_created_at", columnList = "created_at")
    }
)
@SQLDelete(sql = "UPDATE folders SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Folder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vault_id", nullable = false)
    private Vault vault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 80)
    private String icon;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}