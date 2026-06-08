package com.linkvault.tags.controller;

import com.linkvault.common.response.ApiResponse;
import com.linkvault.tags.dto.TagRequest;
import com.linkvault.tags.dto.TagResponse;
import com.linkvault.tags.service.TagService;
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
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/api/tags")
    public ApiResponse<List<TagResponse>> list() {
        return ApiResponse.success("Tags loaded", tagService.listTags());
    }

    @GetMapping("/api/workspaces/{workspaceId}/tags")
    public ApiResponse<List<TagResponse>> listInWorkspace(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Tags loaded", tagService.listTags(workspaceId));
    }

    @PostMapping("/api/tags")
    public ApiResponse<TagResponse> create(@Valid @RequestBody TagRequest request) {
        return ApiResponse.success("Tag created", tagService.create(request));
    }

    @PostMapping("/api/workspaces/{workspaceId}/tags")
    public ApiResponse<TagResponse> createInWorkspace(
        @PathVariable UUID workspaceId,
        @Valid @RequestBody TagRequest request
    ) {
        return ApiResponse.success("Tag created", tagService.create(workspaceId, request));
    }

    @PutMapping("/api/tags/{id}")
    public ApiResponse<TagResponse> update(@PathVariable UUID id, @Valid @RequestBody TagRequest request) {
        return ApiResponse.success("Tag updated", tagService.update(id, request));
    }

    @PutMapping("/api/workspaces/{workspaceId}/tags/{id}")
    public ApiResponse<TagResponse> updateInWorkspace(
        @PathVariable UUID workspaceId,
        @PathVariable UUID id,
        @Valid @RequestBody TagRequest request
    ) {
        return ApiResponse.success("Tag updated", tagService.update(workspaceId, id, request));
    }

    @DeleteMapping("/api/tags/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        tagService.delete(id);
        return ApiResponse.success("Tag deleted", null);
    }

    @DeleteMapping("/api/workspaces/{workspaceId}/tags/{id}")
    public ApiResponse<Void> deleteInWorkspace(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        tagService.delete(workspaceId, id);
        return ApiResponse.success("Tag deleted", null);
    }
}