package com.pawsome.rescue.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_users") // Name of the new table
@Getter
@Setter
public class PendingUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // Store the hashed password here

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

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
    @Column(name = "experience_level")
    private User.ExperienceLevel experienceLevel = User.ExperienceLevel.Beginner; // Reference User enum

    @Column(name = "signed_up_at", updatable = false)
    private LocalDateTime signedUpAt = LocalDateTime.now();
}