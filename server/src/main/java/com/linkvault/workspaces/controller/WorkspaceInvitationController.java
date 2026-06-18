package com.linkvault.workspaces.controller;

import com.linkvault.common.response.ApiResponse;
import com.linkvault.workspaces.dto.WorkspaceInvitationRequest;
import com.linkvault.workspaces.dto.WorkspaceInvitationResponse;
import com.linkvault.workspaces.service.WorkspaceInvitationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService invitationService;

    public WorkspaceInvitationController(WorkspaceInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/api/workspaces/{workspaceId}/invitations")
    public ApiResponse<List<WorkspaceInvitationResponse>> list(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Workspace invitations loaded", invitationService.list(workspaceId));
    }

    @PostMapping("/api/workspaces/{workspaceId}/invitations")
    public ApiResponse<WorkspaceInvitationResponse> invite(
        @PathVariable UUID workspaceId,
        @Valid @RequestBody WorkspaceInvitationRequest request
    ) {
        return ApiResponse.success("Workspace invitation created", invitationService.invite(workspaceId, request));
    }

    @DeleteMapping("/api/workspaces/{workspaceId}/invitations/{invitationId}")
    public ApiResponse<Void> cancel(@PathVariable UUID workspaceId, @PathVariable UUID invitationId) {
        invitationService.cancel(workspaceId, invitationId);
        return ApiResponse.success("Workspace invitation cancelled", null);
    }

    @GetMapping("/api/workspace-invitations/pending")
    public ApiResponse<List<WorkspaceInvitationResponse>> pendingForCurrentUser() {
        return ApiResponse.success("Pending workspace invitations loaded", invitationService.pendingForCurrentUser());
    }

    @PostMapping("/api/workspace-invitations/{token}/accept")
    public ApiResponse<WorkspaceInvitationResponse> accept(@PathVariable String token) {
        return ApiResponse.success("Workspace invitation accepted", invitationService.accept(token));
    }

    @PostMapping("/api/workspace-invitations/{token}/decline")
    public ApiResponse<WorkspaceInvitationResponse> decline(@PathVariable String token) {
        return ApiResponse.success("Workspace invitation declined", invitationService.decline(token));
    }
}
