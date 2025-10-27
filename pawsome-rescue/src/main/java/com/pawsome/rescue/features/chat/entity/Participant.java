package com.pawsome.rescue.features.chat.entity;

import com.pawsome.rescue.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
@Getter
@Setter
public class Participant {

    @EmbeddedId
    private ParticipantId id;

    @ManyToOne(fetch = FetchType.EAGER) // Change FetchType to EAGER
    @MapsId("chatId")
    @JoinColumn(name = "chat_id")
    private ChatGroup chatGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Embeddable
    @Getter
    @Setter
    public static class ParticipantId implements java.io.Serializable {
        @Column(name = "chat_id")
        private String chatId;

        @Column(name = "user_id")
        private Long userId;
    }
}