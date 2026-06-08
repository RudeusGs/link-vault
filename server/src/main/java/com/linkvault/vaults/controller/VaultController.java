package com.linkvault.vaults.controller;

import com.linkvault.common.response.ApiResponse;
import com.linkvault.vaults.dto.VaultRequest;
import com.linkvault.vaults.dto.VaultResponse;
import com.linkvault.vaults.entity.Vault;
import com.linkvault.vaults.service.VaultService;
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
public class VaultController {

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @GetMapping("/api/vaults")
    public ApiResponse<List<VaultResponse>> list() {
        return ApiResponse.success("Vaults loaded", vaultService.listVaults());
    }

    @GetMapping("/api/workspaces/{workspaceId}/vaults")
    public ApiResponse<List<VaultResponse>> listByWorkspace(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Vaults loaded", vaultService.listVaults(workspaceId));
    }

    @GetMapping("/api/vaults/{id}")
    public ApiResponse<VaultResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Vault loaded", vaultService.getVaultResponse(id));
    }

    @GetMapping("/api/workspaces/{workspaceId}/vaults/{id}")
    public ApiResponse<VaultResponse> getByWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        return ApiResponse.success("Vault loaded", vaultService.getVaultResponse(workspaceId, id));
    }

    @PostMapping("/api/vaults")
    public ApiResponse<VaultResponse> create(@Valid @RequestBody VaultRequest request) {
        return ApiResponse.success("Vault created", vaultService.create(request));
    }

    @PostMapping("/api/workspaces/{workspaceId}/vaults")
    public ApiResponse<VaultResponse> createInWorkspace(
        @PathVariable UUID workspaceId,
        @Valid @RequestBody VaultRequest request
    ) {
        return ApiResponse.success("Vault created", vaultService.create(workspaceId, request));
    }

    @PutMapping("/api/vaults/{id}")
    public ApiResponse<VaultResponse> update(@PathVariable UUID id, @Valid @RequestBody VaultRequest request) {
        return ApiResponse.success("Vault updated", vaultService.update(id, request));
    }

    @PutMapping("/api/workspaces/{workspaceId}/vaults/{id}")
    public ApiResponse<VaultResponse> updateInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID id,
        @Valid @RequestBody VaultRequest request
    ) {
        return ApiResponse.success("Vault updated", vaultService.update(workspaceId, id, request));
    }

    @DeleteMapping("/api/vaults/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        vaultService.delete(id);
        return ApiResponse.success("Vault deleted", null);
    }

    @DeleteMapping("/api/workspaces/{workspaceId}/vaults/{id}")
    public ApiResponse<Void> deleteInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        vaultService.delete(workspaceId, id);
        return ApiResponse.success("Vault deleted", null);
    }
}