// File: pawsome-ngo/full/full-d91a39b5e3886f03789eb932561a5689b5f95888/pawsome-rescue/src/main/java/com/pawsome/rescue/features/notification/NotificationDto.java
package com.pawsome.rescue.features.notification;

import com.pawsome.rescue.features.incident.entity.Incident;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationDto {
    private Long id;
    private String message;
    private NotificationType type;
    private Incident.IncidentStatus incidentStatus;
    private Long relatedEntityId;
    private String triggeringUserName; // Display name
    private boolean isRead;
    private LocalDateTime createdAt;
}