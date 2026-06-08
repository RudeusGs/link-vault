package com.linkvault.resources.entity;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.folders.entity.Folder;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.vaults.entity.Vault;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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
    name = "resources",
    indexes = {
        @Index(name = "idx_resources_vault", columnList = "vault_id"),
        @Index(name = "idx_resources_folder", columnList = "folder_id"),
        @Index(name = "idx_resources_type", columnList = "resource_type"),
        @Index(name = "idx_resources_favorite", columnList = "is_favorite"),
        @Index(name = "idx_resources_created_at", columnList = "created_at")
    }
)
public class Resource extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vault_id", nullable = false)
    private Vault vault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType resourceType;

    @Column(length = 2000)
    private String url;

    @Column(length = 2000)
    private String fileUrl;

    @Column(length = 255)
    private String fileName;

    private Long fileSize;

    @Column(length = 150)
    private String mimeType;

    @Column(length = 80)
    private String storageProvider;

    @Column(length = 500)
    private String storageKey;

    @Column(columnDefinition = "text")
    private String content;

    @Column(length = 80)
    private String codeLanguage;

    @Column(length = 255)
    private String sourceName;

    @Column(length = 2000)
    private String thumbnailUrl;

    @Column(length = 500)
    private String previewTitle;

    @Column(length = 1000)
    private String previewDescription;

    @Column(length = 2000)
    private String faviconUrl;

    @Column(length = 255)
    private String siteName;

    @Column(length = 2000)
    private String canonicalUrl;

    private Instant previewFetchedAt;

    @Column(length = 40)
    private String previewStatus;

    @Column(length = 1000)
    private String previewError;

    @Column(nullable = false)
    private Boolean isFavorite = false;

    @Column(nullable = false)
    private Boolean isArchived = false;
}