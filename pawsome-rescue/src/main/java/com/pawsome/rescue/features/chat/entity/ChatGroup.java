package com.pawsome.rescue.features.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_groups")
@Getter
@Setter
public class ChatGroup {

    @Id
    private String id; // UUID

    @Column(nullable = false)
    private String name;

    @Column // The purpose of the chat group (e.g., "INCIDENT", "EVENT")
    private String purpose;

    @Column(name = "purpose_id") // The ID of the related entity (e.g., incident_id)
    private Long purposeId;

    @Column(name = "last_message_id")
    private String lastMessageId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}