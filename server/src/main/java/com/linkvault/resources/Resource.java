package com.linkvault.resources;

import com.linkvault.common.entity.BaseEntity;
import com.linkvault.folders.Folder;
import com.linkvault.vaults.Vault;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "resources")
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

    @Column(nullable = false)
    private Boolean isFavorite = false;

    @Column(nullable = false)
    private Boolean isArchived = false;
}
