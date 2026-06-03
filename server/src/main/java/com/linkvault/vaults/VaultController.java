package com.linkvault.vaults;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vaults")
public class VaultController {

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @GetMapping
    public ApiResponse<List<VaultResponse>> list() {
        return ApiResponse.success("Vaults loaded", vaultService.listVaults());
    }

    @GetMapping("/{id}")
    public ApiResponse<VaultResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Vault loaded", vaultService.getVaultResponse(id));
    }

    @PostMapping
    public ApiResponse<VaultResponse> create(@Valid @RequestBody VaultRequest request) {
        return ApiResponse.success("Vault created", vaultService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<VaultResponse> update(@PathVariable UUID id, @Valid @RequestBody VaultRequest request) {
        return ApiResponse.success("Vault updated", vaultService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        vaultService.delete(id);
        return ApiResponse.success("Vault deleted", null);
    }
}
