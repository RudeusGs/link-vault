package com.linkvault.workspaces.service;

public record WorkspacePlanLimits(
    long vaultLimit,
    long memberLimit,
    long storageLimitBytes
) {
}
