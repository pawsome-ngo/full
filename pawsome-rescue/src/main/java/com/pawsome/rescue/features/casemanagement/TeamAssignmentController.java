package com.pawsome.rescue.features.casemanagement;

import com.pawsome.rescue.features.casemanagement.dto.AssignTeamDto;
import com.pawsome.rescue.features.casemanagement.dto.AssignTeamResponseDto;
import com.pawsome.rescue.features.casemanagement.dto.AvailableVolunteerDto;
import com.pawsome.rescue.features.casemanagement.dto.TeamDetailsDto;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/incidents/{incidentId}/assignment")
public class TeamAssignmentController {

    @Autowired
    private TeamAssignmentService teamAssignmentService;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @GetMapping("/volunteers")
    public ResponseEntity<List<AvailableVolunteerDto>> getAvailableVolunteers(@PathVariable Long incidentId) {
        List<AvailableVolunteerDto> volunteers = teamAssignmentService.getAvailableVolunteersForIncident(incidentId);
        return ResponseEntity.ok(volunteers);
    }
    @PostMapping("/assign-team")
    @PreAuthorize("hasAnyAuthority('ROLE_RESCUE_CAPTAIN', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> assignTeam(Authentication authentication, @PathVariable Long incidentId, @RequestBody AssignTeamDto assignTeamDto) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long assignerId = credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();
        try {
            // The return type is now the new DTO
            AssignTeamResponseDto responseDto = teamAssignmentService.assignTeamToIncident(incidentId, assignTeamDto, assignerId);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/team")
    public ResponseEntity<?> getTeamDetails(@PathVariable Long incidentId) {
        try {
            TeamDetailsDto teamDetails = teamAssignmentService.getTeamDetailsByIncidentId(incidentId);
            return ResponseEntity.ok(teamDetails);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✨ ADD THIS ENTIRE NEW ENDPOINT
    @GetMapping("/team/items")
    @PreAuthorize("isAuthenticated()") // Allow any logged-in user to view the team's items
    public ResponseEntity<Set<String>> getTeamKitItems(@PathVariable Long incidentId) {
        Set<String> items = teamAssignmentService.getTeamKitItems(incidentId);
        return ResponseEntity.ok(items);
    }
}