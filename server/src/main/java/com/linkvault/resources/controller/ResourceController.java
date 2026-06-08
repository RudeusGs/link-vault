package com.linkvault.resources.controller;

import com.linkvault.common.response.ApiResponse;
import com.linkvault.resources.dto.DocumentPreviewResponse;
import com.linkvault.resources.dto.LinkPreviewRequest;
import com.linkvault.resources.dto.LinkPreviewResponse;
import com.linkvault.resources.dto.ResourceFileContent;
import com.linkvault.resources.dto.ResourcePreviewResponse;
import com.linkvault.resources.dto.ResourceRequest;
import com.linkvault.resources.dto.ResourceResponse;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.resources.service.LinkPreviewService;
import com.linkvault.resources.service.ResourceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final LinkPreviewService linkPreviewService;

    public ResourceController(ResourceService resourceService, LinkPreviewService linkPreviewService) {
        this.resourceService = resourceService;
        this.linkPreviewService = linkPreviewService;
    }

    @GetMapping("/api/resources")
    public ApiResponse<List<ResourceResponse>> list() {
        return ApiResponse.success("Resources loaded", resourceService.listAll());
    }

    @GetMapping("/api/workspaces/{workspaceId}/resources")
    public ApiResponse<List<ResourceResponse>> listByWorkspace(
        @PathVariable UUID workspaceId,
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
            resourceService.search(workspaceId, keyword, type, tagId, vaultId, folderId, rootOnly, favorite)
        );
    }

    @GetMapping("/api/vaults/{vaultId}/resources")
    public ApiResponse<List<ResourceResponse>> listByVault(@PathVariable UUID vaultId) {
        return ApiResponse.success("Resources loaded", resourceService.listByVault(vaultId));
    }

    @GetMapping("/api/workspaces/{workspaceId}/vaults/{vaultId}/resources")
    public ApiResponse<List<ResourceResponse>> listByWorkspaceVault(
        @PathVariable UUID workspaceId,
        @PathVariable UUID vaultId
    ) {
        return ApiResponse.success("Resources loaded", resourceService.listByVault(workspaceId, vaultId));
    }

    @GetMapping("/api/folders/{folderId}/resources")
    public ApiResponse<List<ResourceResponse>> listByFolder(@PathVariable UUID folderId) {
        return ApiResponse.success("Resources loaded", resourceService.listByFolder(folderId));
    }

    @GetMapping("/api/workspaces/{workspaceId}/folders/{folderId}/resources")
    public ApiResponse<List<ResourceResponse>> listByWorkspaceFolder(
        @PathVariable UUID workspaceId,
        @PathVariable UUID folderId
    ) {
        return ApiResponse.success("Resources loaded", resourceService.listByFolder(workspaceId, folderId));
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

    @GetMapping("/api/workspaces/{workspaceId}/resources/search")
    public ApiResponse<List<ResourceResponse>> searchInWorkspace(
        @PathVariable UUID workspaceId,
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
            resourceService.search(workspaceId, keyword, type, tagId, vaultId, folderId, rootOnly, favorite)
        );
    }

    @GetMapping("/api/resources/{id}")
    public ApiResponse<ResourceResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Resource loaded", resourceService.getResourceResponse(id));
    }

    @GetMapping("/api/workspaces/{workspaceId}/resources/{id}")
    public ApiResponse<ResourceResponse> getInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        return ApiResponse.success("Resource loaded", resourceService.getResourceResponse(workspaceId, id));
    }

    @PostMapping("/api/link-preview")
    public ApiResponse<LinkPreviewResponse> linkPreview(@Valid @RequestBody LinkPreviewRequest request) {
        return ApiResponse.success("Link preview loaded", linkPreviewService.fetch(request.url()));
    }

    @PostMapping("/api/vaults/{vaultId}/resources")
    public ApiResponse<ResourceResponse> createInVault(
        @PathVariable UUID vaultId,
        @Valid @RequestBody ResourceRequest request
    ) {
        return ApiResponse.success("Resource created", resourceService.createInVault(vaultId, request));
    }

    @PostMapping("/api/workspaces/{workspaceId}/vaults/{vaultId}/resources")
    public ApiResponse<ResourceResponse> createInWorkspaceVault(
        @PathVariable UUID workspaceId,
        @PathVariable UUID vaultId,
        @Valid @RequestBody ResourceRequest request
    ) {
        return ApiResponse.success("Resource created", resourceService.createInVault(workspaceId, vaultId, request));
    }

    @PostMapping("/api/folders/{folderId}/resources")
    public ApiResponse<ResourceResponse> createInFolder(
        @PathVariable UUID folderId,
        @Valid @RequestBody ResourceRequest request
    ) {
        return ApiResponse.success("Resource created", resourceService.createInFolder(folderId, request));
    }

    @PostMapping("/api/workspaces/{workspaceId}/folders/{folderId}/resources")
    public ApiResponse<ResourceResponse> createInWorkspaceFolder(
        @PathVariable UUID workspaceId,
        @PathVariable UUID folderId,
        @Valid @RequestBody ResourceRequest request
    ) {
        return ApiResponse.success("Resource created", resourceService.createInFolder(workspaceId, folderId, request));
    }

    @PutMapping("/api/resources/{id}")
    public ApiResponse<ResourceResponse> update(@PathVariable UUID id, @Valid @RequestBody ResourceRequest request) {
        return ApiResponse.success("Resource updated", resourceService.update(id, request));
    }

    @PutMapping("/api/workspaces/{workspaceId}/resources/{id}")
    public ApiResponse<ResourceResponse> updateInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID id,
        @Valid @RequestBody ResourceRequest request
    ) {
        return ApiResponse.success("Resource updated", resourceService.update(workspaceId, id, request));
    }

    @DeleteMapping("/api/resources/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        resourceService.delete(id);
        return ApiResponse.success("Resource deleted", null);
    }

    @DeleteMapping("/api/workspaces/{workspaceId}/resources/{id}")
    public ApiResponse<Void> deleteInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        resourceService.delete(workspaceId, id);
        return ApiResponse.success("Resource deleted", null);
    }

    @PatchMapping("/api/resources/{id}/favorite")
    public ApiResponse<ResourceResponse> favorite(@PathVariable UUID id) {
        return ApiResponse.success("Favorite updated", resourceService.toggleFavorite(id));
    }

    @PatchMapping("/api/workspaces/{workspaceId}/resources/{id}/favorite")
    public ApiResponse<ResourceResponse> favoriteInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        return ApiResponse.success("Favorite updated", resourceService.toggleFavorite(workspaceId, id));
    }

    @PatchMapping("/api/resources/{id}/archive")
    public ApiResponse<ResourceResponse> archive(@PathVariable UUID id) {
        return ApiResponse.success("Archive updated", resourceService.toggleArchive(id));
    }

    @PatchMapping("/api/workspaces/{workspaceId}/resources/{id}/archive")
    public ApiResponse<ResourceResponse> archiveInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        return ApiResponse.success("Archive updated", resourceService.toggleArchive(workspaceId, id));
    }

    @PatchMapping("/api/resources/{id}/refresh-preview")
    public ApiResponse<ResourceResponse> refreshPreview(@PathVariable UUID id) {
        return ApiResponse.success("Link preview refreshed", resourceService.refreshLinkPreview(id));
    }

    @PatchMapping("/api/workspaces/{workspaceId}/resources/{id}/refresh-preview")
    public ApiResponse<ResourceResponse> refreshPreviewInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID id
    ) {
        return ApiResponse.success("Link preview refreshed", resourceService.refreshLinkPreview(workspaceId, id));
    }

    @PostMapping("/api/resources/{id}/view")
    public ApiResponse<ResourceResponse> view(@PathVariable UUID id) {
        return ApiResponse.success("Resource view recorded", resourceService.recordView(id));
    }

    @PostMapping("/api/workspaces/{workspaceId}/resources/{id}/view")
    public ApiResponse<ResourceResponse> viewInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        return ApiResponse.success("Resource view recorded", resourceService.recordView(workspaceId, id));
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

    @PostMapping(
        value = "/api/workspaces/{workspaceId}/vaults/{vaultId}/resources/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<ResourceResponse> uploadToWorkspaceVault(
        @PathVariable UUID workspaceId,
        @PathVariable UUID vaultId,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String description,
        @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(
            "File resource uploaded",
            resourceService.uploadFileToVault(workspaceId, vaultId, title, description, file)
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

    @PostMapping(
        value = "/api/workspaces/{workspaceId}/folders/{folderId}/resources/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<ResourceResponse> uploadToWorkspaceFolder(
        @PathVariable UUID workspaceId,
        @PathVariable UUID folderId,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String description,
        @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(
            "File resource uploaded",
            resourceService.uploadFileToFolder(workspaceId, folderId, title, description, file)
        );
    }

    @GetMapping("/api/resources/{id}/preview")
    public ApiResponse<ResourcePreviewResponse> preview(@PathVariable UUID id) {
        return ApiResponse.success("Resource preview loaded", resourceService.preview(id));
    }

    @GetMapping("/api/workspaces/{workspaceId}/resources/{id}/preview")
    public ApiResponse<ResourcePreviewResponse> previewInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID id
    ) {
        return ApiResponse.success("Resource preview loaded", resourceService.preview(workspaceId, id));
    }

    @GetMapping("/api/resources/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable UUID id) {
        ResourceFileContent file = resourceService.fileContent(id);
        MediaType mediaType = MediaType.parseMediaType(file.mimeType());
        ContentDisposition disposition = ContentDisposition.inline()
            .filename(file.fileName() == null ? "resource-file" : file.fileName())
            .build();

        return ResponseEntity.ok()
            .contentType(mediaType)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(file.content());
    }

    @GetMapping("/api/workspaces/{workspaceId}/resources/{id}/file")
    public ResponseEntity<byte[]> fileInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        ResourceFileContent file = resourceService.fileContent(workspaceId, id);
        MediaType mediaType = MediaType.parseMediaType(file.mimeType());
        ContentDisposition disposition = ContentDisposition.inline()
            .filename(file.fileName() == null ? "resource-file" : file.fileName())
            .build();

        return ResponseEntity.ok()
            .contentType(mediaType)
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(file.content());
    }

    @GetMapping("/api/resources/{id}/document-preview")
    public ApiResponse<DocumentPreviewResponse> documentPreview(@PathVariable UUID id) {
        return ApiResponse.success("Document preview loaded", resourceService.documentPreview(id));
    }

    @GetMapping("/api/workspaces/{workspaceId}/resources/{id}/document-preview")
    public ApiResponse<DocumentPreviewResponse> documentPreviewInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID id
    ) {
        return ApiResponse.success("Document preview loaded", resourceService.documentPreview(workspaceId, id));
    }

    @PostMapping("/api/resources/{resourceId}/tags/{tagId}")
    public ApiResponse<ResourceResponse> attachTag(@PathVariable UUID resourceId, @PathVariable UUID tagId) {
        return ApiResponse.success("Tag attached", resourceService.attachTag(resourceId, tagId));
    }

    @PostMapping("/api/workspaces/{workspaceId}/resources/{resourceId}/tags/{tagId}")
    public ApiResponse<ResourceResponse> attachTagInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID resourceId,
        @PathVariable UUID tagId
    ) {
        return ApiResponse.success("Tag attached", resourceService.attachTag(workspaceId, resourceId, tagId));
    }

    @DeleteMapping("/api/resources/{resourceId}/tags/{tagId}")
    public ApiResponse<ResourceResponse> detachTag(@PathVariable UUID resourceId, @PathVariable UUID tagId) {
        return ApiResponse.success("Tag removed", resourceService.detachTag(resourceId, tagId));
    }

    @DeleteMapping("/api/workspaces/{workspaceId}/resources/{resourceId}/tags/{tagId}")
    public ApiResponse<ResourceResponse> detachTagInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID resourceId,
        @PathVariable UUID tagId
    ) {
        return ApiResponse.success("Tag removed", resourceService.detachTag(workspaceId, resourceId, tagId));
    }
}