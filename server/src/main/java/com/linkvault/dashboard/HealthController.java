package com.linkvault.dashboard;

import com.linkvault.common.response.ApiResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(
            "LinkVault API is running",
            Map.of(
                "application", "LinkVault",
                "status", "UP",
                "timestamp", Instant.now()
            )
        );
    }
}
