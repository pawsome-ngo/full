// File: pawsome-ngo/full/full-d91a39b5e3886f03789eb932561a5689b5f95888/pawsome-rescue/src/main/java/com/pawsome/rescue/features/notification/NotificationService.java
package com.pawsome.rescue.features.notification;

import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.UserRepository;
import com.pawsome.rescue.features.incident.entity.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);


    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired private PushNotificationService pushNotificationService;

    @Transactional
    public void createNotification(User recipient, NotificationType type, Incident.IncidentStatus incidentStatus, String message, Long relatedEntityId, User triggeringUser) {
        if (recipient == null) {
            logger.warn("Attempted to create notification with null recipient. Type: {}, Message: {}", type, message);
            return;
        }
        try {
            // ... (existing logic to create and save notification entity)
            Notification notification = new Notification();
            notification.setRecipientUser(recipient);
            notification.setType(type);
            notification.setMessage(message);
            notification.setRelatedEntityId(relatedEntityId);
            notification.setTriggeringUser(triggeringUser);
            // ... (set isRead, createdAt, incidentStatus) ...
            if (type == NotificationType.INCIDENT) {
                notification.setIncidentStatus(incidentStatus);
            }

            notificationRepository.save(notification);
            logger.info("Notification created for user {}: {}", recipient.getId(), message);

            // --- ✨ Send the Web Push Notification ---
            // We use the 'message' as the payload
            pushNotificationService.sendNotificationToUser(recipient, message);
            // --- End Send Push ---

        } catch (Exception e) {
            logger.error("Error creating notification for user {}: {}", recipient.getId(), e.getMessage(), e);
        }
    }

    /**
     * Fetches notifications for a given user ID.
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsForUser(Long userId, boolean unreadOnly) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByRecipientUserAndIsReadFalseOrderByCreatedAtDesc(user);
        } else {
            notifications = notificationRepository.findByRecipientUserOrderByCreatedAtDesc(user);
        }

        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Marks a specific notification as read for the user.
     */
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Optional<Notification> notificationOpt = notificationRepository.findByIdAndRecipientUser(notificationId, user);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            if (!notification.isRead()) {
                notification.setRead(true);
                notificationRepository.save(notification);
                return true;
            }
        }
        return false; // Not found or already read
    }

    /**
     * Marks all unread notifications for a user as read.
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        List<Notification> unreadNotifications = notificationRepository.findByRecipientUserAndIsReadFalse(user);
        if (unreadNotifications.isEmpty()) {
            return 0;
        }

        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
        return unreadNotifications.size();
    }

    /**
     * Deletes notifications older than the specified number of days.
     * @param daysOld The minimum age in days for notifications to be deleted.
     * @return The number of notifications deleted.
     */
    @Transactional
    public int deleteNotificationsOlderThan(int daysOld) {
        if (daysOld <= 0) {
            throw new IllegalArgumentException("Number of days must be positive.");
        }
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = notificationRepository.deleteByCreatedAtBefore(cutoffDate);
        logger.info("Deleted {} notifications older than {} days (before {}).", deletedCount, daysOld, cutoffDate);
        return deletedCount;
    }


    // --- ✨ Focus on this method ---
    private NotificationDto convertToDto(Notification notification) {
        if (notification == null) {
            return null;
        }
        // Log the entity data *before* conversion
        logger.debug("Converting Notification ID: {}. Entity incidentStatus: {}",
                notification.getId(), notification.getIncidentStatus());

        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());

        // --- Explicitly map the incidentStatus ---
        // This is the crucial line. Ensure the getter is accessible and the value isn't null here.
        Incident.IncidentStatus statusFromEntity = notification.getIncidentStatus();
        dto.setIncidentStatus(statusFromEntity);
        // --- End mapping ---

        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        if (notification.getTriggeringUser() != null) {
            dto.setTriggeringUserName(notification.getTriggeringUser().getFirstName());
        }

        // Log the DTO data *after* conversion
        logger.debug("Converted DTO for ID: {}. DTO incidentStatus: {}",
                dto.getId(), dto.getIncidentStatus());

        return dto;
    }
    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Find first to ensure ownership before deleting
        Optional<Notification> notificationOpt = notificationRepository.findByIdAndRecipientUser(notificationId, user);
        if (notificationOpt.isPresent()) {
            notificationRepository.delete(notificationOpt.get());
            logger.info("Deleted notification ID {} for user ID {}", notificationId, userId);
            return true;
        } else {
            logger.warn("Attempted to delete notification ID {} for user ID {}, but it was not found or doesn't belong to the user.", notificationId, userId);
            return false; // Not found or doesn't belong to user
        }
    }

    /**
     * Deletes ALL notifications for a specific user.
     * @return The number of notifications deleted.
     */
    @Transactional
    public int deleteAllNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Fetch notifications first to count them before deleting (optional)
        List<Notification> userNotifications = notificationRepository.findByRecipientUserOrderByCreatedAtDesc(user);
        int count = userNotifications.size();

        if (count > 0) {
            notificationRepository.deleteByRecipientUser(user);
            logger.info("Deleted {} notifications for user ID {}", count, userId);
        } else {
            logger.info("No notifications found to delete for user ID {}", userId);
        }
        return count;
    }


}