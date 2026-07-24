package com.linkvault.sharing.entity;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.common.enums.PublicAccess;
import com.linkvault.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "share_links")
public class ShareLink extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false, length = 50)
    private String targetType; // VAULT, FOLDER, RESOURCE

    @Column(nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublicAccess accessLevel;

    private String passwordHash;

    private Instant expiresAt;

    @Column(nullable = false)
    private boolean isRevoked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(nullable = false)
    private int accessCount = 0;

    private Instant lastAccessedAt;
}
