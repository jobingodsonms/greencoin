package com.greencoin.service;

import com.greencoin.model.WasteReport;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyNewReport(WasteReport report) {
        messagingTemplate.convertAndSend("/topic/reports", createLightweightReportMap(report));
    }

    public void notifyStatusChange(WasteReport report) {
        Object payload = createLightweightReportMap(report);
        messagingTemplate.convertAndSend("/topic/report-status/" + report.getId(), payload);
        // Also notify general topic for dashboard updates
        messagingTemplate.convertAndSend("/topic/reports", payload);
    }

    private java.util.Map<String, Object> createLightweightReportMap(WasteReport report) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", report.getId());
        map.put("status", report.getStatus().name());
        map.put("latitude", report.getLatitude());
        map.put("longitude", report.getLongitude());
        map.put("reporterName", report.getReporter().getDisplayName());
        map.put("coinsAwarded", report.getCoinsAwarded());
        // EXCLUDE LARGE IMAGE DATA
        return map;
    }
}
