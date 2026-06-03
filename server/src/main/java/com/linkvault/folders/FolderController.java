package com.linkvault.folders;

import com.linkvault.common.response.ApiResponse;
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

    @GetMapping("/api/folders/{id}")
    public ApiResponse<FolderResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Folder loaded", folderService.getFolderResponse(id));
    }

    @GetMapping("/api/folders/{id}/children")
    public ApiResponse<List<FolderResponse>> children(@PathVariable UUID id) {
        return ApiResponse.success("Child folders loaded", folderService.listChildren(id));
    }

    @PostMapping("/api/folders")
    public ApiResponse<FolderResponse> create(@Valid @RequestBody FolderRequest request) {
        return ApiResponse.success("Folder created", folderService.create(request));
    }

    @PutMapping("/api/folders/{id}")
    public ApiResponse<FolderResponse> update(@PathVariable UUID id, @Valid @RequestBody FolderRequest request) {
        return ApiResponse.success("Folder updated", folderService.update(id, request));
    }

    @DeleteMapping("/api/folders/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        folderService.delete(id);
        return ApiResponse.success("Folder deleted", null);
    }
}
