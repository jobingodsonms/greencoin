package com.greencoin.service;

import com.greencoin.model.User;
import com.greencoin.model.WasteReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyNewReport(WasteReport report) {
        messagingTemplate.convertAndSend("/topic/reports/new", createLightweightReportMap(report));
    }

    public void notifyStatusChange(WasteReport report) {
        Object payload = createLightweightReportMap(report);
        messagingTemplate.convertAndSend("/topic/reports/" + report.getId() + "/status", payload);
        // Also notify general topic for dashboard updates (sync with "new" if needed)
        messagingTemplate.convertAndSend("/topic/reports/new", payload);
    }

    public void notifyCoinUpdate(User user, Integer amount, Integer newBalance) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", amount > 0 ? "COINS_AWARDED" : "COINS_REDEEMED");
        payload.put("amount", amount);
        payload.put("newBalance", newBalance);

        log.info("Sending coin update to user {}: {} coins. New balance: {}",
                user.getEmail(), amount, newBalance);

        messagingTemplate.convertAndSendToUser(user.getFirebaseUid(), "/queue/coins", payload);
    }

    private Map<String, Object> createLightweightReportMap(WasteReport report) {
        Map<String, Object> map = new HashMap<>();
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
