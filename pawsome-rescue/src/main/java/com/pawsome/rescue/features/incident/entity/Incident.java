package com.pawsome.rescue.features.incident.entity;

import com.pawsome.rescue.features.casemanagement.entity.RescueCase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.FetchType; // Ensure FetchType is imported

@Entity
@Table(name = "incidents")
@Getter
@Setter
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "informer_name", nullable = false)
    private String informerName;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column // New field for location text
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "animal_type", nullable = false)
    private AnimalType animalType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "closing_reason", columnDefinition = "TEXT")
    private String closingReason;

    @Column(name = "case_count", nullable = false, columnDefinition = "int default 0")
    private int caseCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private IncidentStatus status = IncidentStatus.REPORTED;

    // --- MODIFIED LINE ---
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.LAZY) // Changed EAGER to LAZY
    private List<IncidentMedia> mediaFiles = new ArrayList<>();
    // --- END MODIFICATION ---

    @Column(name = "reported_at", updatable = false)
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RescueCase> cases = new ArrayList<>();


    public enum AnimalType {
        DOG, CAT, CATTLE, BIRD, OTHER
    }

    public enum IncidentStatus {
        REPORTED,
        ASSIGNED,
        IN_PROGRESS,
        ONGOING,
        RESOLVED,
        CLOSED
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}