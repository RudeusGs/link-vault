package com.linkvault.resources.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkvault.AbstractIntegrationTest;
import com.linkvault.common.outbox.OutboxEvent;
import com.linkvault.common.outbox.OutboxRepository;
import com.linkvault.common.rabbitmq.event.LinkPreviewRequestedEvent;
import com.linkvault.resources.dto.LinkPreviewResponse;
import com.linkvault.resources.dto.ResourceRequest;
import com.linkvault.resources.dto.ResourceResponse;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.users.entity.User;
import com.linkvault.users.repository.UserRepository;
import com.linkvault.users.service.UserContextService;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.vaults.repository.VaultRepository;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.entity.WorkspaceMember;
import com.linkvault.workspaces.repository.WorkspaceRepository;
import com.linkvault.workspaces.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LinkPreviewIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private VaultRepository vaultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserContextService userContextService;

    @MockitoBean
    private LinkPreviewService linkPreviewService;

    private User testUser;
    private Vault testVault;

    @BeforeEach
    void setup() {
        outboxRepository.deleteAll();
        resourceRepository.deleteAll();
        vaultRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setDisplayName("Test");
        testUser.setPasswordHash("hash");
        testUser = userRepository.save(testUser);

        Workspace workspace = new Workspace();
        workspace.setName("Test Workspace");
        workspace.setSlug("test-workspace");
        workspace.setOwner(testUser);
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(testUser);
        member.setRole(com.linkvault.workspaces.enums.WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        testVault = new Vault();
        testVault.setName("Test Vault");
        testVault.setWorkspace(workspace);
        testVault.setUser(testUser);
        testVault = vaultRepository.save(testVault);

        when(userContextService.getCurrentUser()).thenReturn(testUser);
        when(linkPreviewService.normalizeUserUrl(any(String.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void testResourceCreationAndOutbox() throws Exception {
        ResourceRequest request = new ResourceRequest(
            "Test Link", null, ResourceType.LINK, "https://example.com", null, null, null, null, null
        );

        ResourceResponse response = resourceService.createInVault(testVault.getId(), request);

        assertThat(response.id()).isNotNull();

        List<OutboxEvent> outboxEvents = outboxRepository.findAll().stream()
            .filter(e -> "LinkPreviewRequestedEvent".equals(e.getEventType()))
            .toList();
        assertThat(outboxEvents).hasSize(1);
        OutboxEvent event = outboxEvents.get(0);
        
        assertThat(event.getAggregateId()).isEqualTo(response.id());
        
        System.out.println("PAYLOAD: " + event.getPayload());
        LinkPreviewRequestedEvent payload = objectMapper.readValue(event.getPayload(), LinkPreviewRequestedEvent.class);
        assertThat(payload.resourceId()).isEqualTo(response.id());
        assertThat(payload.url()).isEqualTo("https://example.com");
    }
    
    @Test
    void testStaleEventDoesNotOverwrite() {
        Resource resource = new Resource();
        resource.setVault(testVault);
        resource.setTitle("Stale Test");
        resource.setResourceType(ResourceType.LINK);
        resource.setUrl("https://new.com");
        resource = resourceRepository.save(resource);
        
        resourceService.processLinkPreview(resource.getId(), "https://old.com", true);
        
        Resource updated = resourceRepository.findById(resource.getId()).orElseThrow();
        assertThat(updated.getPreviewFetchedAt()).isNull();
    }
    
    @Test
    void testSuccessfulPreviewChangesStatus() {
        Resource resource = new Resource();
        resource.setVault(testVault);
        resource.setTitle("Success Test");
        resource.setResourceType(ResourceType.LINK);
        resource.setUrl("https://example.com");
        resource.setPreviewStatus("PENDING");
        resource = resourceRepository.save(resource);
        
        when(linkPreviewService.fetch("https://example.com", true))
            .thenReturn(new LinkPreviewResponse(
                "https://example.com", "https://example.com", "example", "example.com",
                null, "Title", "Desc", "fav", null, null,
                Instant.now(), "OK", null
            ));
            
        resourceService.processLinkPreview(resource.getId(), "https://example.com", true);
        
        Resource updated = resourceRepository.findById(resource.getId()).orElseThrow();
        assertThat(updated.getPreviewStatus()).isEqualTo("OK");
        assertThat(updated.getPreviewTitle()).isEqualTo("Title");
    }
    
    @Test
    void testPermanentPreviewFailureChangesStatus() {
        Resource resource = new Resource();
        resource.setVault(testVault);
        resource.setTitle("Fail Test");
        resource.setResourceType(ResourceType.LINK);
        resource.setUrl("https://fail.com");
        resource.setPreviewStatus("PENDING");
        resource = resourceRepository.save(resource);
        
        resourceService.markLinkPreviewFailed(resource.getId(), "Connection timeout");
        
        Resource updated = resourceRepository.findById(resource.getId()).orElseThrow();
        assertThat(updated.getPreviewStatus()).isEqualTo("FAILED");
        assertThat(updated.getPreviewError()).isEqualTo("Connection timeout");
    }
}
