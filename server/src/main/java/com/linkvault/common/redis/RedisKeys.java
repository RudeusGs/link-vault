package com.linkvault.common.redis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String workspaceDashboard(UUID workspaceId) {
        return "workspace:" + workspaceId + ":dashboard-summary";
    }

    public static String workspaceVaultList(UUID workspaceId) {
        return "workspace:" + workspaceId + ":vaults";
    }

    public static String workspaceTags(UUID workspaceId) {
        return "workspace:" + workspaceId + ":tags";
    }

    public static String workspaceUsage(UUID workspaceId) {
        return "workspace:" + workspaceId + ":usage";
    }

    public static String workspaceResources(UUID workspaceId, String signature) {
        return "workspace:" + workspaceId + ":resources:" + sha256(signature);
    }

    public static String vaultFolders(UUID vaultId) {
        return "vault:" + vaultId + ":folders";
    }

    public static String folderChildren(UUID folderId) {
        return "folder:" + folderId + ":children";
    }

    public static String resourceDetail(UUID resourceId) {
        return "resource:" + resourceId + ":detail";
    }

    public static String linkPreview(String normalizedUrl) {
        return "link-preview:" + sha256(normalizedUrl);
    }

    public static String rateLimitAuth(String clientKey) {
        return "rate-limit:auth:" + sha256(clientKey);
    }

    public static String rateLimitUpload(String clientKey) {
        return "rate-limit:upload:" + sha256(clientKey);
    }

    public static String rateLimitPreview(String clientKey) {
        return "rate-limit:preview:" + sha256(clientKey);
    }

    public static String workspacePattern(UUID workspaceId) {
        return "workspace:" + workspaceId + ":*";
    }

    public static String vaultPattern(UUID vaultId) {
        return "vault:" + vaultId + ":*";
    }

    public static String folderPattern(UUID folderId) {
        return "folder:" + folderId + ":*";
    }

    public static String resourcePattern(UUID resourceId) {
        return "resource:" + resourceId + ":*";
    }

    public static String pageSignature(Object... values) {
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            builder.append(value == null ? "null" : value).append('|');
        }
        return builder.toString();
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
