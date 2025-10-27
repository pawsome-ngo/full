package com.pawsome.rescue.features.chat.entity;

import com.pawsome.rescue.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
public class Message {

    @Id
    private String id; // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatGroup chatGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // --- MODIFIED ---
    @Column(columnDefinition = "TEXT") // No longer nullable = false
    private String text;
    // --- END MODIFICATION ---

    @Column(name = "client_message_id")
    private String clientMessageId;

    @Column(name = "parent_message_id")
    private String parentMessageId;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    // --- NEW FIELDS ---
    @Column(name = "media_url") // Stores the path/URL returned by storage service
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type") // IMAGE, VIDEO, AUDIO
    private MediaType mediaType;

    public enum MediaType {
        IMAGE, VIDEO, AUDIO
    }
    // --- END NEW FIELDS ---
}