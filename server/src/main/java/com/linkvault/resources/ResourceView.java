package com.linkvault.resources;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
    name = "resource_views",
    indexes = {
        @Index(name = "idx_resource_views_resource", columnList = "resource_id"),
        @Index(name = "idx_resource_views_user", columnList = "user_id"),
        @Index(name = "idx_resource_views_viewed_at", columnList = "viewed_at")
    }
)
public class ResourceView extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant viewedAt = Instant.now();
}
