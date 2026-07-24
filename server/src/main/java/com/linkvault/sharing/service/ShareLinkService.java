package com.linkvault.sharing.service;

import com.linkvault.common.enums.PublicAccess;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.exception.UnauthorizedException;
import com.linkvault.sharing.entity.ShareLink;
import com.linkvault.sharing.repository.ShareLinkRepository;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import com.linkvault.vaults.repository.VaultRepository;
import com.linkvault.folders.repository.FolderRepository;
import com.linkvault.resources.repository.ResourceRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final UserContextService userContextService;
    private final VaultRepository vaultRepository;
    private final FolderRepository folderRepository;
    private final ResourceRepository resourceRepository;

    public ShareLinkService(
        ShareLinkRepository shareLinkRepository,
        UserContextService userContextService,
        VaultRepository vaultRepository,
        FolderRepository folderRepository,
        ResourceRepository resourceRepository
    ) {
        this.shareLinkRepository = shareLinkRepository;
        this.userContextService = userContextService;
        this.vaultRepository = vaultRepository;
        this.folderRepository = folderRepository;
        this.resourceRepository = resourceRepository;
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Transactional
    public String createShareLink(String targetType, UUID targetId, PublicAccess accessLevel, Instant expiresAt) {
        validateTargetExists(targetType, targetId);

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        ShareLink link = new ShareLink();
        link.setTokenHash(tokenHash);
        link.setTargetType(targetType);
        link.setTargetId(targetId);
        link.setAccessLevel(accessLevel);
        link.setExpiresAt(expiresAt);
        User user = userContextService.getCurrentUser();
        link.setCreatedBy(user);
        shareLinkRepository.save(link);

        return rawToken;
    }

    @Transactional
    public ShareLink validateAndRecordAccess(String rawToken, String expectedTargetType, UUID expectedTargetId, boolean requireEdit) {
        String tokenHash = hashToken(rawToken);
        ShareLink link = shareLinkRepository.findByTokenHashAndDeletedAtIsNull(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("Invalid share token"));

        if (!link.getTargetType().equals(expectedTargetType) || !link.getTargetId().equals(expectedTargetId)) {
            throw new UnauthorizedException("Share token is not for this resource");
        }

        if (link.isRevoked()) {
            throw new UnauthorizedException("Share link has been revoked");
        }

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Share link has expired");
        }

        if (requireEdit && link.getAccessLevel() != PublicAccess.EDIT) {
            throw new UnauthorizedException("Share link does not have edit permissions");
        }

        link.setAccessCount(link.getAccessCount() + 1);
        link.setLastAccessedAt(Instant.now());
        shareLinkRepository.save(link);

        return link;
    }

    private void validateTargetExists(String targetType, UUID targetId) {
        boolean exists = switch (targetType.toUpperCase()) {
            case "VAULT" -> vaultRepository.existsById(targetId);
            case "FOLDER" -> folderRepository.existsById(targetId);
            case "RESOURCE" -> resourceRepository.existsById(targetId);
            default -> throw new com.linkvault.common.exception.BadRequestException("Invalid target type");
        };

        if (!exists) {
            throw new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, targetType + " not found");
        }
    }
}
