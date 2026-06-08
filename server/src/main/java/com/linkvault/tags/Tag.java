package com.linkvault.tags;

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
    name = "tags",
    uniqueConstraints = @UniqueConstraint(name = "uk_tags_user_name", columnNames = {"user_id", "name"}),
    indexes = {
        @Index(name = "idx_tags_user", columnList = "user_id"),
        @Index(name = "idx_tags_created_at", columnList = "created_at")
    }
)
public class Tag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 40)
    private String color;
}
