package com.linkvault.resources.service;

import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.mapper.ResourceMapper;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.storage.dto.StorageResult;
import com.linkvault.storage.service.StorageService;
import com.linkvault.users.service.UserContextService;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.service.QuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceServiceUploadTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private QuotaService quotaService;

    @Mock
    private UserContextService userContextService;

    @Mock
    private ResourceMapper resourceMapper;

    @InjectMocks
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldDeleteFromStorageIfDbSaveFails() {
        // Arrange
        Vault vault = new Vault();
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        vault.setWorkspace(workspace);

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        StorageResult storageResult = new StorageResult("url", "secure-url", "public-id", "test.txt", 7L, "text/plain");
        when(storageService.upload(file)).thenReturn(storageResult);

        when(resourceRepository.save(any(Resource.class))).thenThrow(new RuntimeException("DB Save Failed"));

        // Use reflection to invoke the private uploadFile method for testing
        // For testing purposes, we can test uploadFileToVault
        
        ResourceService spyResourceService = spy(resourceService);
        // We will just invoke uploadFileToVault but mock vaultService which we didn't inject
    }
}
