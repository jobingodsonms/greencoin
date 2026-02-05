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
        messagingTemplate.convertAndSend("/topic/reports", report);
    }

    public void notifyStatusChange(WasteReport report) {
        messagingTemplate.convertAndSend("/topic/report-status/" + report.getId(), report);
        // Also notify general topic for dashboard updates
        messagingTemplate.convertAndSend("/topic/reports", report);
    }
}
