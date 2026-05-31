package com.qalytix.controller;

import com.qalytix.dto.request.CreateNotificationConfigRequest;
import com.qalytix.dto.response.ApiResponse;
import com.qalytix.dto.response.NotificationConfigResponse;
import com.qalytix.dto.response.NotificationEventResponse;
import com.qalytix.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<NotificationConfigResponse>>> listConfigs() {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.listConfigs()));
    }

    @PostMapping("/configs")
    public ResponseEntity<ApiResponse<NotificationConfigResponse>> createConfig(
            @Valid @RequestBody CreateNotificationConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.createConfig(request)));
    }

    @PutMapping("/configs/{id}")
    public ResponseEntity<ApiResponse<NotificationConfigResponse>> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody CreateNotificationConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.updateConfig(id, request)));
    }

    @DeleteMapping("/configs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable Long id) {
        notificationService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted"));
    }

    @PostMapping("/configs/{id}/test")
    public ResponseEntity<ApiResponse<Void>> testConfig(@PathVariable Long id) {
        notificationService.testConfig(id);
        return ResponseEntity.ok(ApiResponse.ok("Test notification sent"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<NotificationEventResponse>>> history(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.listHistory(limit)));
    }
}
