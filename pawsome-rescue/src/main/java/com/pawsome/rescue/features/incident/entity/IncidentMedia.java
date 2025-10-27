// File: pawsome-rescue/src/main/java/com/pawsome/rescue/features/incident/entity/IncidentMedia.java
package com.pawsome.rescue.features.incident.entity;

import com.pawsome.rescue.features.casemanagement.entity.RescueCase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "incident_media")
@Getter
@Setter
public class IncidentMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    // --- MODIFICATION: Allow case_id to be null ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id") // Removed nullable = false
    private RescueCase rescueCase;
    // --- END MODIFICATION ---

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(nullable = false)
    private Boolean isInitialReport = false; // Default to false
    // --- END MODIFICATION ---

    public enum MediaType {
        IMAGE, VIDEO, AUDIO
    }
}