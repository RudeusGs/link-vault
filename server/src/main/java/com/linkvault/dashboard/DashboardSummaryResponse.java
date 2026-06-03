package com.linkvault.dashboard;

import com.linkvault.resources.ResourceResponse;
import com.linkvault.tags.TagResponse;
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
