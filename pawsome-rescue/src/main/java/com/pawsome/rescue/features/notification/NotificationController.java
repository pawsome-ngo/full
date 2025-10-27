// File: pawsome-ngo/full/full-d91a39b5e3886f03789eb932561a5689b5f95888/pawsome-rescue/src/main/java/com/pawsome/rescue/features/notification/NotificationController.java
package com.pawsome.rescue.features.notification;

import com.pawsome.rescue.auth.repository.CredentialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CredentialsRepository credentialsRepository; // To get userId from username

    private Long getUserIdFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getUser().getId();
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getMyNotifications(
            Authentication authentication,
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            List<NotificationDto> notifications = notificationService.getNotificationsForUser(userId, unreadOnly);
            return ResponseEntity.ok(notifications);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(null); // User not found
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markNotificationAsRead(Authentication authentication, @PathVariable Long notificationId) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            boolean updated = notificationService.markAsRead(notificationId, userId);
            if (updated) {
                return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
            } else {
                // Either not found for this user or already read
                return ResponseEntity.status(404).body(Map.of("message", "Notification not found or already read."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage())); // User not found
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "An error occurred."));
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllNotificationsAsRead(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            int count = notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(Map.of("message", count + " notifications marked as read."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage())); // User not found
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "An error occurred."));
        }
    }

    // Example: DELETE /api/notifications/older-than/7 (deletes notifications older than 7 days)
    @DeleteMapping("/older-than/{days}")
    public ResponseEntity<?> deleteOldNotifications(@PathVariable int days) {
        // Add security check here - Only allow ADMIN/SUPER_ADMIN?
        // Example: @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
        if (days <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Number of days must be positive."));
        }
        try {
            int count = notificationService.deleteNotificationsOlderThan(days);
            return ResponseEntity.ok(Map.of("message", "Deleted " + count + " notifications older than " + days + " days."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "An error occurred during deletion."));
        }
    }

    // --- ✨ New DELETE Endpoint (Single Notification) ---
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(Authentication authentication, @PathVariable Long notificationId) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            boolean deleted = notificationService.deleteNotification(notificationId, userId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Notification deleted successfully."));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Notification not found or you don't have permission to delete it."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage())); // User not found
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "An error occurred during deletion."));
        }
    }
    // --- End New Endpoint ---

    // --- ✨ New DELETE Endpoint (All Notifications for User) ---
    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllMyNotifications(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            int count = notificationService.deleteAllNotificationsForUser(userId);
            return ResponseEntity.ok(Map.of("message", "Deleted " + count + " notifications."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage())); // User not found
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "An error occurred during deletion."));
        }
    }
}