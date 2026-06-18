package com.linkvault.common.redis;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheInvalidationService {

    private final RedisCacheService cacheService;

    public RedisCacheInvalidationService(RedisCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public void invalidateWorkspace(UUID workspaceId) {
        if (workspaceId == null) {
            return;
        }
        cacheService.deleteByPattern(RedisKeys.workspacePattern(workspaceId));
    }

    public void invalidateVault(UUID vaultId) {
        if (vaultId == null) {
            return;
        }
        cacheService.deleteByPattern(RedisKeys.vaultPattern(vaultId));
    }

    public void invalidateFolder(UUID folderId) {
        if (folderId == null) {
            return;
        }
        cacheService.deleteByPattern(RedisKeys.folderPattern(folderId));
    }

    public void invalidateResource(UUID resourceId) {
        if (resourceId == null) {
            return;
        }
        cacheService.deleteByPattern(RedisKeys.resourcePattern(resourceId));
    }
}
