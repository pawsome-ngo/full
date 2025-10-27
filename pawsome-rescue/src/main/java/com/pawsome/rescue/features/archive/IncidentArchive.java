package com.pawsome.rescue.features.archive;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_archive") // This table will not be related to any others
@Getter
@Setter
public class IncidentArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_incident_id", nullable = false)
    private Long originalIncidentId; // The ID from the old incidents table

    @Column(name = "informer_name", nullable = false)
    private String informerName;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Column(columnDefinition = "TEXT")
    private String location;

    @Column(name = "animal_type", nullable = false)
    private String animalType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "final_status", nullable = false)
    private String finalStatus; // e.g., "RESOLVED", "CLOSED"

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt = LocalDateTime.now();

    @Column(name = "closing_reason", columnDefinition = "TEXT")
    private String closingReason; // Reason for "CLOSED" status

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes; // Notes from all closed cases

    @Column(name = "involved_members", columnDefinition = "TEXT")
    private String involvedMembers; // Comma-separated list of member names
}