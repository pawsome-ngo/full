package com.pawsome.rescue.features.incident;

import com.pawsome.rescue.auth.repository.CredentialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents/{incidentId}/interest")
public class InterestController {

    @Autowired
    private InterestService interestService;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @PostMapping
    public ResponseEntity<?> expressInterest(Authentication authentication, @PathVariable Long incidentId) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        try {
            interestService.expressInterest(incidentId, userId);
            return ResponseEntity.ok().body("Interest expressed successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> removeInterest(Authentication authentication, @PathVariable Long incidentId) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        interestService.removeInterest(incidentId, userId);
        return ResponseEntity.ok().body("Interest removed successfully.");
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> removeAllInterestsForIncident(@PathVariable Long incidentId) {
        interestService.removeAllInterestsForIncident(incidentId);
        return ResponseEntity.ok().body("All interests for incident " + incidentId + " have been removed.");
    }
}