package com.linkvault.workspaces.controller;

import com.linkvault.common.response.ApiResponse;
import com.linkvault.workspaces.dto.WorkspaceMemberResponse;
import com.linkvault.workspaces.dto.WorkspaceMemberRoleRequest;
import com.linkvault.workspaces.dto.WorkspaceRequest;
import com.linkvault.workspaces.dto.WorkspaceResponse;
import com.linkvault.workspaces.dto.WorkspaceUsageResponse;
import com.linkvault.workspaces.service.WorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ApiResponse<List<WorkspaceResponse>> list() {
        return ApiResponse.success("Workspaces loaded", workspaceService.listCurrentUserWorkspaces());
    }

    @PostMapping
    public ApiResponse<WorkspaceResponse> create(@Valid @RequestBody WorkspaceRequest request) {
        return ApiResponse.success("Workspace created", workspaceService.create(request));
    }

    @GetMapping("/{workspaceId}")
    public ApiResponse<WorkspaceResponse> get(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Workspace loaded", workspaceService.getWorkspaceResponse(workspaceId));
    }

    @PutMapping("/{workspaceId}")
    public ApiResponse<WorkspaceResponse> update(
        @PathVariable UUID workspaceId,
        @Valid @RequestBody WorkspaceRequest request
    ) {
        return ApiResponse.success("Workspace updated", workspaceService.update(workspaceId, request));
    }

    @DeleteMapping("/{workspaceId}")
    public ApiResponse<Void> delete(@PathVariable UUID workspaceId) {
        workspaceService.delete(workspaceId);
        return ApiResponse.success("Workspace deleted", null);
    }

    @GetMapping("/{workspaceId}/members")
    public ApiResponse<List<WorkspaceMemberResponse>> members(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Workspace members loaded", workspaceService.listMembers(workspaceId));
    }

    @PatchMapping("/{workspaceId}/members/{memberId}/role")
    public ApiResponse<WorkspaceMemberResponse> updateMemberRole(
        @PathVariable UUID workspaceId,
        @PathVariable UUID memberId,
        @Valid @RequestBody WorkspaceMemberRoleRequest request
    ) {
        return ApiResponse.success(
            "Workspace member role updated",
            workspaceService.updateMemberRole(workspaceId, memberId, request)
        );
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public ApiResponse<Void> removeMember(@PathVariable UUID workspaceId, @PathVariable UUID memberId) {
        workspaceService.removeMember(workspaceId, memberId);
        return ApiResponse.success("Workspace member removed", null);
    }

    @GetMapping("/{workspaceId}/usage")
    public ApiResponse<WorkspaceUsageResponse> usage(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Workspace usage loaded", workspaceService.usage(workspaceId));
    }
}