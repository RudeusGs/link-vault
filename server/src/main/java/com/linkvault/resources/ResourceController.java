package com.linkvault.resources;

import com.linkvault.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/api/resources")
    public ApiResponse<List<ResourceResponse>> list() {
        return ApiResponse.success("Resources loaded", resourceService.listAll());
    }

    @GetMapping("/api/vaults/{vaultId}/resources")
    public ApiResponse<List<ResourceResponse>> listByVault(@PathVariable UUID vaultId) {
        return ApiResponse.success("Resources loaded", resourceService.listByVault(vaultId));
    }

    @GetMapping("/api/folders/{folderId}/resources")
    public ApiResponse<List<ResourceResponse>> listByFolder(@PathVariable UUID folderId) {
        return ApiResponse.success("Resources loaded", resourceService.listByFolder(folderId));
    }

    @GetMapping("/api/resources/search")
    public ApiResponse<List<ResourceResponse>> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) ResourceType type,
        @RequestParam(required = false) UUID tagId,
        @RequestParam(required = false) UUID vaultId,
        @RequestParam(required = false) UUID folderId,
        @RequestParam(required = false) Boolean rootOnly,
        @RequestParam(required = false) Boolean favorite
    ) {
        return ApiResponse.success(
            "Resources loaded",
            resourceService.search(keyword, type, tagId, vaultId, folderId, rootOnly, favorite)
        );
    }

    @GetMapping("/api/resources/{id}")
    public ApiResponse<ResourceResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Resource loaded", resourceService.getResourceResponse(id));
    }

    @PostMapping("/api/vaults/{vaultId}/resources")
    public ApiResponse<ResourceResponse> createInVault(
        @PathVariable UUID vaultId,
        @Valid @RequestBody ResourceRequest request
    ) {
        return ApiResponse.success("Resource created", resourceService.createInVault(vaultId, request));
    }

    @PostMapping("/api/folders/{folderId}/resources")
    public ApiResponse<ResourceResponse> createInFolder(
        @PathVariable UUID folderId,
        @Valid @RequestBody ResourceRequest request
    ) {
        return ApiResponse.success("Resource created", resourceService.createInFolder(folderId, request));
    }

    @PutMapping("/api/resources/{id}")
    public ApiResponse<ResourceResponse> update(@PathVariable UUID id, @Valid @RequestBody ResourceRequest request) {
        return ApiResponse.success("Resource updated", resourceService.update(id, request));
    }

    @DeleteMapping("/api/resources/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        resourceService.delete(id);
        return ApiResponse.success("Resource deleted", null);
    }

    @PatchMapping("/api/resources/{id}/favorite")
    public ApiResponse<ResourceResponse> favorite(@PathVariable UUID id) {
        return ApiResponse.success("Favorite updated", resourceService.toggleFavorite(id));
    }

    @PatchMapping("/api/resources/{id}/archive")
    public ApiResponse<ResourceResponse> archive(@PathVariable UUID id) {
        return ApiResponse.success("Archive updated", resourceService.toggleArchive(id));
    }

    @PostMapping("/api/resources/{id}/view")
    public ApiResponse<ResourceResponse> view(@PathVariable UUID id) {
        return ApiResponse.success("Resource view recorded", resourceService.recordView(id));
    }

    @PostMapping(value = "/api/vaults/{vaultId}/resources/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResourceResponse> uploadToVault(
        @PathVariable UUID vaultId,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String description,
        @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(
            "File resource uploaded",
            resourceService.uploadFileToVault(vaultId, title, description, file)
        );
    }

    @PostMapping(value = "/api/folders/{folderId}/resources/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResourceResponse> uploadToFolder(
        @PathVariable UUID folderId,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String description,
        @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(
            "File resource uploaded",
            resourceService.uploadFileToFolder(folderId, title, description, file)
        );
    }

    @GetMapping("/api/resources/{id}/preview")
    public ApiResponse<ResourcePreviewResponse> preview(@PathVariable UUID id) {
        return ApiResponse.success("Resource preview loaded", resourceService.preview(id));
    }

    @PostMapping("/api/resources/{resourceId}/tags/{tagId}")
    public ApiResponse<ResourceResponse> attachTag(@PathVariable UUID resourceId, @PathVariable UUID tagId) {
        return ApiResponse.success("Tag attached", resourceService.attachTag(resourceId, tagId));
    }

    @DeleteMapping("/api/resources/{resourceId}/tags/{tagId}")
    public ApiResponse<ResourceResponse> detachTag(@PathVariable UUID resourceId, @PathVariable UUID tagId) {
        return ApiResponse.success("Tag removed", resourceService.detachTag(resourceId, tagId));
    }
}
