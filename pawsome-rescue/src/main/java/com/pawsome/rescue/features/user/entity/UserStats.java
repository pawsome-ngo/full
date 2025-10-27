package com.pawsome.rescue.features.user.entity;

import com.pawsome.rescue.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_stats")
@Getter
@Setter
public class UserStats {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int points = 0;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int hearts = 0;

    @Column(name = "distance_traveled", nullable = false, columnDefinition = "double default 0.0")
    private double distanceTraveled = 0.0;

    @Column(name = "cases_completed", nullable = false, columnDefinition = "int default 0")
    private int casesCompleted = 0;
}