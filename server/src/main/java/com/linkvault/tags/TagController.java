package com.linkvault.tags;

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
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> list() {
        return ApiResponse.success("Tags loaded", tagService.listTags());
    }

    @PostMapping
    public ApiResponse<TagResponse> create(@Valid @RequestBody TagRequest request) {
        return ApiResponse.success("Tag created", tagService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TagResponse> update(@PathVariable UUID id, @Valid @RequestBody TagRequest request) {
        return ApiResponse.success("Tag updated", tagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        tagService.delete(id);
        return ApiResponse.success("Tag deleted", null);
    }
}
