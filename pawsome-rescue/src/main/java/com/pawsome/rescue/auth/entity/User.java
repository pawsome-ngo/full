package com.pawsome.rescue.auth.entity;

import com.pawsome.rescue.features.user.entity.UserStats;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    public enum Position {
        FOUNDER, CO_FOUNDER, PRESIDENT, VICE_PRESIDENT,
        GENERAL_SECRETARY, ASSISTANT_GENERAL_SECRETARY,
        EXECUTIVE, ASSOCIATE, MEMBER,
        FEEDING_CAPTAIN,
        ADOPTION_COORDINATOR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserStats userStats;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Credentials credentials;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRole> userRoles;

    @Enumerated(EnumType.STRING)
    // --- THIS IS THE FIX ---
    // Explicitly set a column length that can accommodate the longest enum name
    @Column(name = "position", length = 50)
    private Position position = Position.MEMBER;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "address")
    private String address;

    @Column(name = "motivation", columnDefinition = "TEXT")
    private String motivation;

    @Column(name = "has_vehicle")
    private Boolean hasVehicle = false;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "can_provide_shelter")
    private Boolean canProvideShelter = false;

    @Column(name = "has_medicine_box")
    private Boolean hasMedicineBox = false;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status")
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.Unavailable;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experienceLevel = ExperienceLevel.Beginner;

    @Column(name = "joined_since")
    private LocalDateTime joinedSince = LocalDateTime.now();

    public enum AvailabilityStatus {
        Available, Unavailable
    }

    public enum ExperienceLevel {
        Beginner, Intermediate, Advanced, Expert
    }
}