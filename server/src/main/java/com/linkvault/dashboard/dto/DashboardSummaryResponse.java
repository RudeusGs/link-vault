package com.linkvault.dashboard.dto;

import com.linkvault.resources.dto.ResourceResponse;
import com.linkvault.tags.dto.TagResponse;
import java.util.List;

public record DashboardSummaryResponse(
    long totalVaults,
    long totalFolders,
    long totalResources,
    long totalLinks,
    long totalFiles,
    long totalNotes,
    long totalSnippets,
    long totalFavorites,
    List<ResourceResponse> recentResources,
    List<TagResponse> topTags
) {
}