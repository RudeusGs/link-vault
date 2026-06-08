package com.linkvault.vaults;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
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
    name = "vaults",
    uniqueConstraints = @UniqueConstraint(name = "uk_vaults_user_name", columnNames = {"user_id", "name"}),
    indexes = {
        @Index(name = "idx_vaults_user", columnList = "user_id"),
        @Index(name = "idx_vaults_created_at", columnList = "created_at")
    }
)
public class Vault extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 80)
    private String icon;

    @Column(length = 40)
    private String color;
}
