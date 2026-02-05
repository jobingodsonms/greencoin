package com.greencoin.service;

import com.greencoin.dto.CreateReportRequest;
import com.greencoin.dto.WasteReportResponse;
import com.greencoin.model.User;
import com.greencoin.model.WasteReport;
import com.greencoin.repository.WasteReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WasteReportService {

    private final WasteReportRepository reportRepository;
    private final UserService userService;
    private final CoinService coinService;
    private final WebSocketService webSocketService;

    @Transactional
    public WasteReport createReport(CreateReportRequest request, String firebaseUid) {
        User reporter = userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WasteReport report = WasteReport.builder()
                .reporter(reporter)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .status(WasteReport.ReportStatus.OPEN)
                .coinsAwarded(10) // Fixed amount or based on logic
                .reportedAt(LocalDateTime.now())
                .build();

        WasteReport savedReport = reportRepository.save(report);
        webSocketService.notifyNewReport(savedReport);
        return savedReport;
    }

    public List<WasteReport> getAvailableReports() {
        return reportRepository.findByStatus(WasteReport.ReportStatus.OPEN);
    }

    public List<WasteReport> getNearbyReports(Double lat, Double lon) {
        return reportRepository.findNearby(lat, lon, 10.0); // 10km radius
    }

    public List<WasteReport> getReportsByUser(Long userId) {
        return reportRepository.findByReporterId(userId);
    }

    public List<WasteReport> getReportsByCollector(Long collectorId) {
        return reportRepository.findByCollectorId(collectorId);
    }

    public WasteReport getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
    }

    @Transactional
    public WasteReport markPicking(Long reportId, String firebaseUid) {
        User collector = userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("Collector not found"));

        WasteReport report = getReportById(reportId);
        if (report.getStatus() != WasteReport.ReportStatus.OPEN) {
            throw new RuntimeException("Report is not available for picking");
        }

        report.setStatus(WasteReport.ReportStatus.PICKING);
        report.setCollector(collector);
        report.setPickedAt(LocalDateTime.now());

        WasteReport saved = reportRepository.save(report);
        webSocketService.notifyStatusChange(saved);
        return saved;
    }

    @Transactional
    public WasteReport markCollected(Long reportId, String firebaseUid) {
        WasteReport report = getReportById(reportId);
        if (report.getStatus() != WasteReport.ReportStatus.PICKING) {
            throw new RuntimeException("Report must be in PICKING status to be marked as collected");
        }

        report.setStatus(WasteReport.ReportStatus.COLLECTED);
        report.setCollectedAt(LocalDateTime.now());

        WasteReport saved = reportRepository.save(report);

        // Award coins to reporter
        coinService.awardCoins(report.getReporter(), report.getCoinsAwarded(), report.getId());

        webSocketService.notifyStatusChange(saved);
        return saved;
    }
}
