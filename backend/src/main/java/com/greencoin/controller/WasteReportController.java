package com.greencoin.controller;

import com.greencoin.dto.CreateReportRequest;
import com.greencoin.dto.WasteReportResponse;
import com.greencoin.model.User;
import com.greencoin.model.WasteReport;
import com.greencoin.service.UserService;
import com.greencoin.service.WasteReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Waste Report Controller
 * 
 * Endpoints for citizens and collectors to manage waste reports.
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class WasteReportController {

    private final WasteReportService reportService;
    private final UserService userService;

    /**
     * Create new waste report (CITIZEN only)
     */
    @PostMapping
    public ResponseEntity<WasteReportResponse> createReport(
            Authentication authentication,
            @Valid @RequestBody CreateReportRequest request) {

        String firebaseUid = authentication.getName();

        WasteReport report = reportService.createReport(request, firebaseUid);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToResponse(report));
    }

    /**
     * Get all available reports (OPEN status)
     * Used by collectors to see pickupable reports
     */
    @GetMapping("/available")
    public ResponseEntity<List<WasteReportResponse>> getAvailableReports() {
        List<WasteReport> reports = reportService.getAvailableReports();
        List<WasteReportResponse> response = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get nearby reports (within ~5km)
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<WasteReportResponse>> getNearbyReports(
            @RequestParam java.math.BigDecimal latitude,
            @RequestParam java.math.BigDecimal longitude) {

        List<WasteReport> reports = reportService.getNearbyReports(latitude.doubleValue(), longitude.doubleValue());
        List<WasteReportResponse> response = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get current user's reports (reporter view)
     */
    @GetMapping("/my-reports")
    public ResponseEntity<List<WasteReportResponse>> getMyReports(Authentication authentication) {
        String firebaseUid = authentication.getName();
        User user = userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<WasteReport> reports = reportService.getReportsByUser(user.getId());
        List<WasteReportResponse> response = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get reports picked by collector (COLLECTOR only)
     */
    @GetMapping("/my-pickups")
    public ResponseEntity<List<WasteReportResponse>> getMyPickups(Authentication authentication) {
        String firebaseUid = authentication.getName();
        User user = userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.hasRole(User.UserRole.COLLECTOR)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<WasteReport> reports = reportService.getReportsByCollector(user.getId());
        List<WasteReportResponse> response = reports.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Mark report as PICKING (COLLECTOR only)
     */
    @PatchMapping("/{reportId}/pick")
    public ResponseEntity<Void> markPicking(
            Authentication authentication,
            @PathVariable Long reportId) {

        String firebaseUid = authentication.getName();
        reportService.markPicking(reportId, firebaseUid);

        return ResponseEntity.ok().build();
    }

    /**
     * Mark report as COLLECTED (COLLECTOR only)
     * This awards coins to the reporter
     */
    @PatchMapping("/{reportId}/collect")
    public ResponseEntity<Void> markCollected(
            Authentication authentication,
            @PathVariable Long reportId) {

        String firebaseUid = authentication.getName();
        reportService.markCollected(reportId, firebaseUid);

        return ResponseEntity.ok().build();
    }

    /**
     * Get single report details
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<WasteReportResponse> getReport(@PathVariable Long reportId) {
        WasteReport report = reportService.getReportById(reportId);
        return ResponseEntity.ok(mapToResponse(report));
    }

    /**
     * Map entity to DTO
     */
    private WasteReportResponse mapToResponse(WasteReport report) {
        return WasteReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getDisplayName())
                .latitude(report.getLatitude())
                .longitude(report.getLongitude())
                .imageUrl(report.getImageUrl())
                .description(report.getDescription())
                .status(report.getStatus().name())
                .coinsAwarded(report.getCoinsAwarded())
                .collectorId(report.getCollector() != null ? report.getCollector().getId() : null)
                .collectorName(report.getCollector() != null ? report.getCollector().getDisplayName() : null)
                .reportedAt(report.getReportedAt())
                .collectedAt(report.getCollectedAt())
                .build();
    }
}
