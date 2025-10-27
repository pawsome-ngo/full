package com.pawsome.rescue.features.casemanagement;

import com.pawsome.rescue.features.casemanagement.dto.CaseCompletionDetailsDto;
import com.pawsome.rescue.features.incident.dto.IncidentDto;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.pawsome.rescue.features.casemanagement.dto.InitiateCaseDto;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
public class RescueCaseController {

    @Autowired
    private RescueCaseService rescueCaseService;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @PostMapping("/{incidentId}/initiate-confirm")
    public ResponseEntity<?> confirmCaseInitiation(Authentication authentication, @PathVariable Long incidentId, @RequestBody InitiateCaseDto initiateDto) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long initiatorUserId = credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        try {
            rescueCaseService.confirmInitiation(incidentId, initiateDto.getParticipatingUserIds(), initiatorUserId);
            // Return a simple success message, the frontend will update the status locally
            return ResponseEntity.ok().body(Map.of("message", "Case initiated successfully."));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Log the exception for debugging
            // logger.error("Error confirming case initiation for incident {}: {}", incidentId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "An unexpected error occurred."));
        }
    }

    @GetMapping("/my-cases")
    public ResponseEntity<List<IncidentDto>> getMyCases(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        List<IncidentDto> myCases = rescueCaseService.getMyCases(userId);
        return ResponseEntity.ok(myCases);
    }

    @PostMapping(value = "/{incidentId}/close", consumes = {"multipart/form-data"})
    public ResponseEntity<?> closeCase(@PathVariable Long incidentId,
                                       @RequestPart("details") CaseCompletionDetailsDto details,
                                       @RequestPart(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles) {
        try {
            rescueCaseService.closeCase(incidentId, details, mediaFiles);
            return ResponseEntity.ok().body("Case closed successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}