package io.shub.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Simulates sending notifications (emails).
 *
 * <p>In demo mode ({@code notification.simulate-failure=true}), this service
 * intentionally throws an exception to trigger the saga compensation flow.</p>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final boolean simulateFailure;

    public NotificationService(@Value("${notification.simulate-failure:false}") boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    /**
     * Send a notification email.
     *
     * @param userId  the target user
     * @param email   the email address
     * @param subject the email subject
     * @param message the email body
     * @throws RuntimeException if simulating failure (demo mode)
     */
    public void sendNotification(String userId, String email, String subject, String message) {
        log.info("Sending notification: userId={}, email={}, subject='{}'", userId, email, subject);

        if (simulateFailure) {
            log.error("SIMULATED FAILURE: Notification service is configured to fail for demo purposes");
            throw new RuntimeException("Simulated notification failure — email service unavailable");
        }

        // In a real implementation, this would call an email API (SendGrid, SES, etc.)
        log.info("Notification sent successfully: userId={}, email={}, subject='{}'",
                userId, email, subject);
    }
}
