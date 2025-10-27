package com.pawsome.rescue.features.notification;

import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
import com.pawsome.rescue.auth.repository.UserRepository;
import jakarta.transaction.Transactional; // --- IMPORT @Transactional ---
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class PushNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationController.class);

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;
    @Autowired
    private CredentialsRepository credentialsRepository;
    @Autowired
    private UserRepository userRepository;

    @Getter
    @Setter
    static class SubscriptionRequest {
        private String endpoint;
        private String p256dh;
        private String auth;
    }

    // --- MODIFIED METHOD ---
    @PostMapping("/subscribe")
    @Transactional // Make this method transactional
    public ResponseEntity<?> subscribe(Authentication authentication, @RequestBody SubscriptionRequest request) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // --- 1. DELETE ALL EXISTING SUBSCRIPTIONS FOR THIS USER ---
            logger.debug("Deleting all existing push subscriptions for user ID: {}", userId);
            pushSubscriptionRepository.deleteByUser(user);
            logger.info("Successfully deleted old subscriptions for user ID: {}", userId);

            // --- 2. REMOVED the 'existsByEndpoint' check as it's no longer needed ---
            // if (pushSubscriptionRepository.existsByEndpoint(request.getEndpoint())) { ... }

            // --- 3. SAVE THE NEW SUBSCRIPTION ---
            PushSubscription subscription = new PushSubscription();
            subscription.setUser(user);
            subscription.setEndpoint(request.getEndpoint());
            subscription.setP256dh(request.getP256dh());
            subscription.setAuth(request.getAuth());

            pushSubscriptionRepository.save(subscription);
            logger.info("New push subscription saved for user ID {}. Endpoint: {}", userId, request.getEndpoint());

            return ResponseEntity.ok(Map.of("message", "Subscription successful."));
        } catch (Exception e) {
            logger.error("Failed to save subscription for user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to save subscription."));
        }
    }
    // --- END MODIFIED METHOD ---

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> request) {
        try {
            String endpoint = request.get("endpoint");
            if (endpoint == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Endpoint is required."));
            }

            // This method is fine, as it's for explicitly unsubscribing a *specific* device
            pushSubscriptionRepository.deleteByEndpoint(endpoint);
            logger.info("Push subscription removed: {}", endpoint);

            return ResponseEntity.ok(Map.of("message", "Unsubscribed successfully."));
        } catch (Exception e) {
            logger.error("Failed to unsubscribe endpoint: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to unsubscribe."));
        }
    }


    private Long getUserIdFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getUser().getId();
    }
}