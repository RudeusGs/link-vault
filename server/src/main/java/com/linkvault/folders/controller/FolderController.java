package com.linkvault.folders.controller;

import com.linkvault.common.response.ApiResponse;
import com.linkvault.folders.dto.FolderRequest;
import com.linkvault.folders.dto.FolderResponse;
import com.linkvault.folders.service.FolderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping("/api/vaults/{vaultId}/folders")
    public ApiResponse<List<FolderResponse>> listByVault(@PathVariable UUID vaultId) {
        return ApiResponse.success("Folders loaded", folderService.listByVault(vaultId));
    }

    @GetMapping("/api/workspaces/{workspaceId}/vaults/{vaultId}/folders")
    public ApiResponse<List<FolderResponse>> listByWorkspaceVault(
        @PathVariable UUID workspaceId,
        @PathVariable UUID vaultId
    ) {
        return ApiResponse.success("Folders loaded", folderService.listByVault(workspaceId, vaultId));
    }

    @GetMapping("/api/folders/{id}")
    public ApiResponse<FolderResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Folder loaded", folderService.getFolderResponse(id));
    }

    @GetMapping("/api/workspaces/{workspaceId}/folders/{id}")
    public ApiResponse<FolderResponse> getInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        return ApiResponse.success("Folder loaded", folderService.getFolderResponse(workspaceId, id));
    }

    @GetMapping("/api/folders/{id}/children")
    public ApiResponse<List<FolderResponse>> children(@PathVariable UUID id) {
        return ApiResponse.success("Child folders loaded", folderService.listChildren(id));
    }

    @GetMapping("/api/workspaces/{workspaceId}/folders/{id}/children")
    public ApiResponse<List<FolderResponse>> childrenInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID id
    ) {
        return ApiResponse.success("Child folders loaded", folderService.listChildren(workspaceId, id));
    }

    @PostMapping("/api/vaults/{vaultId}/folders")
    public ApiResponse<FolderResponse> createInVault(
        @PathVariable UUID vaultId,
        @Valid @RequestBody FolderRequest request
    ) {
        return ApiResponse.success("Folder created", folderService.createInVault(vaultId, request));
    }

    @PostMapping("/api/workspaces/{workspaceId}/vaults/{vaultId}/folders")
    public ApiResponse<FolderResponse> createInWorkspaceVault(
        @PathVariable UUID workspaceId,
        @PathVariable UUID vaultId,
        @Valid @RequestBody FolderRequest request
    ) {
        return ApiResponse.success("Folder created", folderService.createInVault(workspaceId, vaultId, request));
    }

    @PostMapping("/api/folders/{parentId}/children")
    public ApiResponse<FolderResponse> createChild(
        @PathVariable UUID parentId,
        @Valid @RequestBody FolderRequest request
    ) {
        return ApiResponse.success("Folder created", folderService.createChild(parentId, request));
    }

    @PostMapping("/api/workspaces/{workspaceId}/folders/{parentId}/children")
    public ApiResponse<FolderResponse> createChildInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID parentId,
        @Valid @RequestBody FolderRequest request
    ) {
        return ApiResponse.success("Folder created", folderService.createChild(workspaceId, parentId, request));
    }

    @PutMapping("/api/folders/{id}")
    public ApiResponse<FolderResponse> update(@PathVariable UUID id, @Valid @RequestBody FolderRequest request) {
        return ApiResponse.success("Folder updated", folderService.update(id, request));
    }

    @PutMapping("/api/workspaces/{workspaceId}/folders/{id}")
    public ApiResponse<FolderResponse> updateInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID id,
        @Valid @RequestBody FolderRequest request
    ) {
        return ApiResponse.success("Folder updated", folderService.update(workspaceId, id, request));
    }

    @DeleteMapping("/api/folders/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        folderService.delete(id);
        return ApiResponse.success("Folder deleted", null);
    }

    @DeleteMapping("/api/workspaces/{workspaceId}/folders/{id}")
    public ApiResponse<Void> deleteInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        folderService.delete(workspaceId, id);
        return ApiResponse.success("Folder deleted", null);
    }
}