package com.linkvault.sharing.controller;

import com.linkvault.common.enums.PublicAccess;
import com.linkvault.common.response.ApiResponse;
import com.linkvault.sharing.service.ShareLinkService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/share-links")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    public ShareLinkController(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    @PostMapping
    public ApiResponse<String> createShareLink(
        @RequestParam String targetType,
        @RequestParam UUID targetId,
        @RequestParam PublicAccess accessLevel,
        @RequestParam(required = false) Instant expiresAt
    ) {
        String token = shareLinkService.createShareLink(targetType, targetId, accessLevel, expiresAt);
        return ApiResponse.success("Share link created", token);
    }
}
