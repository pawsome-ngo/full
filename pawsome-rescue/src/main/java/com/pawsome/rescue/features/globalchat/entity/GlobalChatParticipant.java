package com.pawsome.rescue.features.globalchat.entity;

import com.pawsome.rescue.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "gchat_participants") // New table
@Getter
@Setter
public class GlobalChatParticipant {

    @Id
    private Long userId; // Use the User's ID as the Primary Key

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // This maps the userId field to the User entity's ID
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
}