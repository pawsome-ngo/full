package com.pawsome.rescue.features.incident; // Correct package

import com.pawsome.rescue.features.incident.dto.*;
import com.pawsome.rescue.features.incident.entity.Incident;
import com.pawsome.rescue.features.user.dto.UpdateLocationDto;
import jakarta.validation.Valid; // Import Valid
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private static final Logger logger = LoggerFactory.getLogger(IncidentController.class);

    @Autowired
    private IncidentService incidentService;

    @GetMapping
    public ResponseEntity<List<IncidentDto>> getAllIncidents() {
        List<IncidentDto> incidents = incidentService.getAllIncidents();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/summary")
    public ResponseEntity<List<IncidentSummaryDto>> getAllIncidentsSummary() {
        List<IncidentSummaryDto> incidents = incidentService.getAllIncidentsSummary();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/live")
    public ResponseEntity<List<IncidentSummaryDto>> getLiveIncidents() {
        List<IncidentSummaryDto> incidents = incidentService.getLiveIncidentsSummary();
        return ResponseEntity.ok(incidents);
    }

    @PostMapping(value = "/report", consumes = {"multipart/form-data"})
    public ResponseEntity<?> reportIncident(@RequestPart("incident") @Valid IncidentReportDto reportDto, // Added @Valid
                                            @RequestPart(value = "media", required = false) List<MultipartFile> mediaFiles) {
        try {
            Incident newIncident = incidentService.reportIncident(reportDto, mediaFiles);
            // Consider returning the created IncidentDto or just the ID/message
            return ResponseEntity.status(HttpStatus.CREATED) // Use 201 Created status
                    .body(Map.of(
                            "message", "Incident reported successfully",
                            "incidentId", newIncident.getId()
                    ));
        } catch (Exception e) {
            logger.error("Failed to report incident: {}", e.getMessage(), e);
            // Provide a more specific error message if possible
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to report incident. " + e.getMessage()));
        }
    }

    @DeleteMapping("/{incidentId}/media/{mediaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // Added Authorization
    public ResponseEntity<?> deleteSingleMedia(@PathVariable Long incidentId, @PathVariable Long mediaId) {
        try {
            boolean deleted = incidentService.deleteSingleMediaItem(incidentId, mediaId);
            // The service now throws exceptions for not found, so no need to check boolean here
            return ResponseEntity.ok(Map.of("message", "Media item deleted successfully."));
        } catch (IllegalArgumentException e) { // Catch specific exceptions from service
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting media item {} for incident {}: {}", mediaId, incidentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while deleting the media item."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentDto> getIncidentById(@PathVariable Long id) {
        Optional<IncidentDto> incident = incidentService.getIncidentById(id);
        return incident.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<IncidentDto>> getIncidentsByStatus(@PathVariable("status") String status) {
        try {
            // Consider moving this parsing logic to the service layer
            Incident.IncidentStatus incidentStatus = Incident.IncidentStatus.valueOf(status.toUpperCase());
            List<IncidentDto> incidents = incidentService.getIncidentsByStatus(incidentStatus);
            return ResponseEntity.ok(incidents);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status value received: {}", status);
            // Return Bad Request with an empty list to match the return type
            // You could also change the return type to ResponseEntity<?> or use @ControllerAdvice
            // to handle the exception globally and return a standardized error response.
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    // --- NEW ENDPOINT TO UPDATE INCIDENT DETAILS ---
    /**
     * Endpoint to update core details of an existing incident.
     * Accessible by admins or rescue captains.
     */
    @PutMapping("/{incidentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'RESCUE_CAPTAIN')") // Authorization check
    public ResponseEntity<IncidentDto> updateIncidentDetails(
            @PathVariable Long incidentId,
            @Valid @RequestBody UpdateIncidentRequest updateRequest) { // Use @Valid DTO

        logger.info("CONTROLLER - Received request to update incident ID: {}", incidentId);
        // Service method now returns DTO directly
        IncidentDto updatedIncident = incidentService.updateIncidentDetails(incidentId, updateRequest);
        logger.info("CONTROLLER - Successfully updated incident ID: {}", incidentId);
        return ResponseEntity.ok(updatedIncident);
    }
    // --- END NEW ENDPOINT ---


    @PutMapping("/{id}/location")
    // Consider adding authorization check if needed
    // @PreAuthorize("...")
    public ResponseEntity<IncidentDto> updateIncidentLocation(@PathVariable Long id, @Valid @RequestBody UpdateLocationDto locationDto) { // Added @Valid
        // Service method now returns DTO directly
        Optional<IncidentDto> updatedIncident = incidentService.updateIncidentLocation(id, locationDto);
        return updatedIncident.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // This endpoint might be redundant if updateIncidentDetails covers status,
    // or keep it if specific status transitions have complex logic/permissions.
    // Ensure DTO and Service logic align with needs.
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'RESCUE_CAPTAIN')")
    public ResponseEntity<Incident> updateIncidentStatus(@PathVariable Long id, @RequestBody UpdateStatusDto statusDto) {
        // Consider validating the status string in the DTO
        try {
            Incident.IncidentStatus newStatus = Incident.IncidentStatus.valueOf(String.valueOf(statusDto.getStatus()).toUpperCase());
            Optional<Incident> updatedIncident = incidentService.updateIncidentStatus(id, newStatus);
            return updatedIncident.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status value provided for incident {}: {}", id, statusDto.getStatus());
            return ResponseEntity.badRequest().body(null); // Or return an error message map
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Or return Map.of("message", e.getMessage())
        }
    }


    // Initiate, Resolve, Close, Reactivate endpoints should ideally check permissions
    // using @PreAuthorize or within the service layer based on user roles and incident state.

    @PutMapping("/{id}/initiate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'RESCUE_CAPTAIN')") // Example authorization
    public ResponseEntity<?> initiateCase(@PathVariable Long id) {
        try {
            Optional<Incident> updatedIncident = incidentService.initiateCase(id);
            return updatedIncident.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'RESCUE_CAPTAIN')") // Example authorization
    public ResponseEntity<?> resolveIncident(@PathVariable Long id) {
        try {
            Optional<Incident> updatedIncident = incidentService.resolveIncident(id);
            return updatedIncident.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'RESCUE_CAPTAIN')") // Example authorization
    public ResponseEntity<?> closeIncident(@PathVariable Long id, @Valid @RequestBody CloseIncidentDto closeDto) { // Added @Valid
        try {
            Optional<Incident> updatedIncident = incidentService.closeIncident(id, closeDto.getReason());
            return updatedIncident.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/history")
    // Add authorization if needed
    public ResponseEntity<IncidentHistoryDto> getIncidentHistory(@PathVariable Long id) {
        try {
            IncidentHistoryDto history = incidentService.getIncidentHistory(id);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) { // Assuming service throws this for not found
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/media")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // Added Authorization
    public ResponseEntity<?> deleteAllMedia(@PathVariable Long id) {
        try {
            incidentService.deleteAllMediaForIncident(id);
            return ResponseEntity.ok().body(Map.of("message", "All media for incident " + id + " deleted successfully."));
        } catch (Exception e) { // Catch broader exceptions
            logger.error("Failed to delete all media for incident {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete all media for incident " + id + "."));
        }
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'RESCUE_CAPTAIN')") // Example authorization
    public ResponseEntity<?> reactivateIncident(@PathVariable Long id) {
        try {
            Optional<Incident> updatedIncident = incidentService.reactivateIncident(id);
            return updatedIncident.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // Added Authorization
    public ResponseEntity<?> deleteIncident(
            @PathVariable Long id,
            @RequestBody(required = false) DeleteIncidentRequestDto request) {

        boolean shouldArchive = request == null || request.isArchive();

        try {
            incidentService.deleteIncident(id, shouldArchive);
            String message = "Incident " + id + " deleted successfully" + (shouldArchive ? " and archived." : ".");
            return ResponseEntity.ok().body(Map.of("message", message));
        } catch (IllegalArgumentException e) { // Assuming service throws this for not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) { // Assuming service throws this for invalid state
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) { // Catch unexpected errors
            logger.error("Failed to delete incident {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred while deleting the incident."));
        }
    }
}