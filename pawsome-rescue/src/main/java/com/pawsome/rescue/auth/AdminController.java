package com.pawsome.rescue.auth;

import com.pawsome.rescue.auth.dto.PendingUserDto;
import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.exception.UnauthorizedActionException;
import com.pawsome.rescue.features.admin.dto.BatchDeleteUserRequestDto;
import com.pawsome.rescue.features.admin.dto.DeleteUserRequestDto;
import com.pawsome.rescue.features.admin.dto.SuperAdminUserDto;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    @Autowired
    private CredentialsRepository credentialsRepository;

    private Long getUserIdFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getUser().getId();
    }


    @GetMapping("/pending-users")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<PendingUserDto>> getPendingUsers() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.findPendingUsers());
    }


    // Use pendingUserId in the path
    @PutMapping("/authorize/{pendingUserId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> authorizeUser(@PathVariable Long pendingUserId) {
        try {
            adminService.authorizeUser(pendingUserId);
            return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("message", "User authorized successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) { // Catch broader exceptions during migration
            logger.error("Error authorizing pending user {}: {}", pendingUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "An error occurred during authorization."));
        }
    }

    @DeleteMapping("/deny/{pendingUserId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> denyUser(@PathVariable Long pendingUserId) {
        try {
            adminService.denyUser(pendingUserId);
            return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("message", "Pending user denied and removed successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", e.getMessage()));
        }
    }



    @PutMapping("/users/{userId}/position")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> updateUserPosition(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        try {
            User.Position position = User.Position.valueOf(body.get("position"));
            adminService.updateUserPosition(userId, position);
            return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("message", "User position updated successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Invalid position or user ID."));
        }
    }

    // --- ✨ NEW ENDPOINT for Experience Level ---
    @PutMapping("/users/{userId}/experience")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> updateUserExperience(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        try {
            User.ExperienceLevel experience = User.ExperienceLevel.valueOf(body.get("experience"));
            adminService.updateUserExperienceLevel(userId, experience);
            return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("message", "User experience level updated successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Invalid experience level or user ID."));
        }
    }
    // --- End New Endpoint ---

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long userId, @RequestBody List<String> roleNames, Authentication authentication) {
        try {
            adminService.updateUserRoles(userId, roleNames, authentication);
            return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("message", "User roles updated successfully."));
        } catch (UnauthorizedActionException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<SuperAdminUserDto>> getAllUsersForSuperAdmin(Authentication authentication) {
        try {
            Long currentUserId = getUserIdFromAuthentication(authentication);
            List<SuperAdminUserDto> users = adminService.getUsersForSuperAdminView(currentUserId);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching users for super admin: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/users/{userIdToDelete}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userIdToDelete,
            @RequestBody(required = false) DeleteUserRequestDto requestDto,
            Authentication authentication) {
        logger.debug("Attempting single delete for user ID: {}", userIdToDelete);
        try {
            Long currentUserId = getUserIdFromAuthentication(authentication);
            boolean notify = (requestDto != null) && requestDto.isNotifyUsers();
            adminService.deleteSingleUserAsSuperAdmin(userIdToDelete, currentUserId, notify);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
        } catch (IllegalStateException e) {
            logger.warn("Single delete conflict for user ID {}: {}", userIdToDelete, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Single delete bad request for user ID {}: {}", userIdToDelete, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during single deleteUser for user ID {}: {}", userIdToDelete, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "An unexpected error occurred during user deletion."));
        }
    }

    @DeleteMapping("/users/batch")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> deleteUsersBatch(
            @RequestBody BatchDeleteUserRequestDto requestDto,
            Authentication authentication) {
        logger.debug("Attempting batch delete for user IDs: {}", requestDto.getUserIds());
        try {
            Long currentUserId = getUserIdFromAuthentication(authentication);
            if (requestDto.getUserIds() == null || requestDto.getUserIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "No user IDs provided for deletion."));
            }
            Map<String, List<Map<String, Object>>> result = adminService.deleteUsersBatch(
                    requestDto.getUserIds(),
                    currentUserId,
                    requestDto.isNotifyUsers()
            );
            String deletedMsg = result.get("deleted").isEmpty() ? "" : result.get("deleted").size() + " user(s) deleted successfully.";
            String skippedMsg = result.get("skipped").isEmpty() ? "" : result.get("skipped").size() + " user(s) skipped.";
            String finalMessage = String.join(" ", deletedMsg, skippedMsg).trim();
            return ResponseEntity.ok(Map.of(
                    "message", finalMessage.isEmpty() ? "No users processed." : finalMessage,
                    "details", result
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Batch delete bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during batch user deletion: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "An unexpected error occurred during batch user deletion."));
        }
    }

    @DeleteMapping("/reset-application-keep-users")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> resetApplicationKeepUsers() {
        logger.warn("Received request to reset application data, keeping users.");
        try {
            adminService.resetApplicationDataKeepUsers();
            return ResponseEntity.ok(Map.of("message", "Application data reset successfully, keeping users and roles."));
        } catch (Exception e) {
            logger.error("Error during reset-keep-users operation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred during the reset process: " + e.getMessage()));
        }
    }

    // --- ADD THIS NEW ENDPOINT ---
    @PutMapping("/users/{userId}/reset-password")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable Long userId,
            Authentication authentication) {

        logger.warn("Received request to reset password for user ID: {}", userId);
        try {
            Long currentSuperAdminId = getUserIdFromAuthentication(authentication);
            adminService.resetUserPassword(userId, currentSuperAdminId);
            return ResponseEntity.ok(Map.of("message", "User password reset successfully to 'pawsome'."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Password reset failed (Bad Request) for user ID {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (UnauthorizedActionException e) {
            logger.warn("Password reset failed (Forbidden) for user ID {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during password reset for user ID {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "An unexpected error occurred."));
        }
    }
    // --- END NEW ENDPOINT ---

    @DeleteMapping("/clear-global-chat") // Using DELETE as it's a destructive action
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> clearGlobalChat(@RequestParam(defaultValue = "100") int keep) {
        // Default to keeping 100 messages if no parameter is provided
        try {
            int deletedCount = adminService.clearGlobalChatMessages(keep);
            return ResponseEntity.ok(Map.of( // Use Map.of for simple JSON response
                    "message", "Global chat cleared successfully.",
                    "messages_deleted", deletedCount,
                    "messages_kept", keep
            ));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for clearing global chat: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error clearing global chat: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred while clearing global chat."));
        }
    }
}