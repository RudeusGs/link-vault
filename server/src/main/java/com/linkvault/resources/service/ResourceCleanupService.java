package com.linkvault.resources.service;

import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.resources.repository.ResourceTagRepository;
import com.linkvault.resources.repository.ResourceViewRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceCleanupService {

    private final ResourceRepository resourceRepository;
    private final ResourceTagRepository resourceTagRepository;
    private final ResourceViewRepository resourceViewRepository;

    public ResourceCleanupService(
        ResourceRepository resourceRepository,
        ResourceTagRepository resourceTagRepository,
        ResourceViewRepository resourceViewRepository
    ) {
        this.resourceRepository = resourceRepository;
        this.resourceTagRepository = resourceTagRepository;
        this.resourceViewRepository = resourceViewRepository;
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
        });
        resourceRepository.deleteAll(resources);
    }
}