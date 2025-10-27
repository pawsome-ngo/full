package com.pawsome.rescue.features.notification;

import com.pawsome.rescue.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "push_subscriptions")
@Getter
@Setter
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 1024)
    private String endpoint; // The unique push service URL

    @Column(nullable = false, length = 256)
    private String p256dh; // The user's public key

    @Column(nullable = false, length = 256)
    private String auth; // The user's auth secret
}