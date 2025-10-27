// File: pawsome-ngo/full/full-d91a39b5e3886f03789eb932561a5689b5f95888/pawsome-rescue/src/main/java/com/pawsome/rescue/features/notification/Notification.java
package com.pawsome.rescue.features.notification;

import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.features.incident.entity.Incident;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipientUser;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_status", length = 50, nullable = true) // Nullable because it only applies to INCIDENT type
    private Incident.IncidentStatus incidentStatus;

    @Column(name = "related_entity_id")
    private Long relatedEntityId; // Optional ID for linking (e.g., incident ID)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggering_user_id") // Nullable
    private User triggeringUser; // Optional: User who caused the notification

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}