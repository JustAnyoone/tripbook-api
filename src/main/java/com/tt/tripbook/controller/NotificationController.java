package com.tt.tripbook.controller;

import com.tt.tripbook.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Triggered by an external cron service (e.g. cron-job.org) via a daily POST.
// The cron caller must include the correct secret token in the X-Notify-Token header.
@RestController
@RequestMapping("/api/notify")
@Tag(name = "notifications", description = "Departure reminders (cron-triggered)")
public class NotificationController {

    private final NotificationService service;
    private final String notifyToken;

    public NotificationController(NotificationService service,
                                  @Value("${NOTIFY_TOKEN:}") String notifyToken) {
        this.service = service;
        this.notifyToken = notifyToken;
    }

    @Operation(summary = "Send departure reminders (cron)",
            description = "Sends email reminders for CONFIRMED trips departing in 3 days. "
                    + "Requires X-Notify-Token header matching the NOTIFY_TOKEN environment variable.")
    @ApiResponse(responseCode = "200", description = "Reminders sent")
    @ApiResponse(responseCode = "401", description = "Invalid or missing token")
    @PostMapping
    public ResponseEntity<Map<String, Object>> sendReminders(
            @RequestHeader(value = "X-Notify-Token", required = false) String token) {

        if (notifyToken.isBlank() || !notifyToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or missing X-Notify-Token"));
        }

        int count = service.sendDepartureReminders();
        return ResponseEntity.ok(Map.of("sent", count));
    }
}
