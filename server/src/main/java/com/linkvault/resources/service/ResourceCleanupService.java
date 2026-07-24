package com.linkvault.resources.service;

import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.resources.repository.ResourceTagRepository;
import com.linkvault.resources.repository.ResourceViewRepository;
import com.linkvault.storage.service.StorageService;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceCleanupService {

    private final ResourceRepository resourceRepository;
    private final ResourceTagRepository resourceTagRepository;
    private final ResourceViewRepository resourceViewRepository;
    private final StorageService storageService;

    public ResourceCleanupService(
        ResourceRepository resourceRepository,
        ResourceTagRepository resourceTagRepository,
        ResourceViewRepository resourceViewRepository,
        StorageService storageService
    ) {
        this.resourceRepository = resourceRepository;
        this.resourceTagRepository = resourceTagRepository;
        this.resourceViewRepository = resourceViewRepository;
        this.storageService = storageService;
    }

    @Transactional
    public void delete(Resource resource) {
        deleteAll(List.of(resource));
    }

    @Transactional
    public void deleteAll(Collection<Resource> resources) {
        resources.forEach(resource -> {
            resourceTagRepository.deleteByResource_Id(resource.getId());
            resourceViewRepository.deleteByResource_Id(resource.getId());
            
            if (resource.getResourceType() == ResourceType.FILE && resource.getStorageKey() != null) {
                try {
                    storageService.delete(resource.getStorageKey(), resource.getMimeType(), resource.getFileName());
                } catch (Exception exception) {
                    org.slf4j.LoggerFactory.getLogger(ResourceCleanupService.class)
                        .error("Failed to delete storage file: " + resource.getStorageKey(), exception);
                }
            }
        });
        resourceRepository.deleteAll(resources);
    }
}