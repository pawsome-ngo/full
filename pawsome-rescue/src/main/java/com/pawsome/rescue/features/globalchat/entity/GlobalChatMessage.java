package com.pawsome.rescue.features.globalchat.entity;

import com.pawsome.rescue.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

// NOTE: This entity is separate from features.chat.entity.Message
@Entity
@Table(name = "gchat_messages") // New table
@Getter
@Setter
public class GlobalChatMessage {

    @Id
    private String id; // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String text; // Nullable for media-only messages

    @Column(name = "client_message_id")
    private String clientMessageId;

    @Column(name = "parent_message_id")
    private String parentMessageId;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "media_url")
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private MediaType mediaType;

    public enum MediaType {
        IMAGE, VIDEO, AUDIO
    }
}