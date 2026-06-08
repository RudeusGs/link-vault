package com.linkvault.audit.controller;

import com.linkvault.audit.dto.AuditLogResponse;
import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/workspaces/{workspaceId}/audit-logs")
    public ApiResponse<List<AuditLogResponse>> list(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Audit logs loaded", auditLogService.list(workspaceId));
    }
}