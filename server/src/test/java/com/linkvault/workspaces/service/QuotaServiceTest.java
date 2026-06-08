package com.linkvault.workspaces.service;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.resources.repository.ResourceRepository;
import com.linkvault.users.entity.User;
import com.linkvault.vaults.repository.VaultRepository;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.enums.WorkspacePlan;
import com.linkvault.workspaces.repository.WorkspaceMemberRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuotaServiceTest {

    private final VaultRepository vaultRepository = mock(VaultRepository.class);
    private final ResourceRepository resourceRepository = mock(ResourceRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final QuotaService quotaService = new QuotaService(
        vaultRepository,
        resourceRepository,
        workspaceMemberRepository
    );

    @Test
    void requireCanCreateVaultRejectsFreeWorkspaceAtLimit() {
        Workspace workspace = workspace();
        workspace.setPlan(WorkspacePlan.FREE);

        when(vaultRepository.countByWorkspace_Id(workspace.getId())).thenReturn(5L);
        when(workspaceMemberRepository.countByWorkspace_IdAndDeletedAtIsNull(workspace.getId())).thenReturn(1L);
        when(resourceRepository.countByVault_Workspace_Id(workspace.getId())).thenReturn(0L);
        when(resourceRepository.sumFileSizeByWorkspaceId(workspace.getId())).thenReturn(0L);

        assertThatThrownBy(() -> quotaService.requireCanCreateVault(workspace))
            .isInstanceOf(BadRequestException.class);
    }

    private Workspace workspace() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("ada");

        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setOwner(owner);
        workspace.setName("Ada Workspace");
        workspace.setSlug("ada");
        return workspace;
    }
}