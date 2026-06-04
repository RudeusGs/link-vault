package com.linkvault.resources;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.tags.Tag;
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
    name = "resource_tags",
    uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "tag_id"}),
    indexes = {
        @Index(name = "idx_resource_tags_resource", columnList = "resource_id"),
        @Index(name = "idx_resource_tags_tag", columnList = "tag_id")
    }
)
public class ResourceTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}


