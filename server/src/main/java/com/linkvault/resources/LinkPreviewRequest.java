package com.linkvault.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkPreviewRequest(
    @NotBlank @Size(max = 2000) String url
) {
}
