package com.pawsome.rescue.features.globalchat.controller;

import com.pawsome.rescue.auth.repository.CredentialsRepository;
import com.pawsome.rescue.exception.UnauthorizedActionException; // Import custom exception
import com.pawsome.rescue.features.globalchat.dto.GlobalChatMessageDto;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatMessage.MediaType;
import com.pawsome.rescue.features.globalchat.repository.GlobalChatParticipantRepository;
import com.pawsome.rescue.features.globalchat.service.GlobalChatService;
import com.pawsome.rescue.features.storage.LocalStorageService;
import com.pawsome.rescue.features.storage.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gchat") // Base path for global chat REST API
public class GlobalChatController {

    private static final Logger logger = LoggerFactory.getLogger(GlobalChatController.class);

    @Autowired private GlobalChatService globalChatService;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private GlobalChatParticipantRepository participantRepository;
    @Autowired private LocalStorageService localStorageService;

    // Helper to get user ID and check participation
    private Long checkParticipantAndGetId(Authentication authentication) throws UnauthorizedActionException {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found for username: " + userDetails.getUsername())).getUser().getId();

        if (!participantRepository.existsById(userId)) {
            logger.warn("User ID {} tried to access global chat but is not a participant.", userId);
            throw new UnauthorizedActionException("User is not a participant of the global chat.");
        }
        logger.debug("User ID {} is a participant.", userId); // Log participation success
        return userId;
    }

    /**
     * Gets all historical messages for the global chat.
     * Requires the user to be a participant.
     */
    @GetMapping("/messages")
    public ResponseEntity<List<GlobalChatMessageDto>> getChatMessages(Authentication authentication) {
        logger.debug("Attempting to fetch global chat messages...");
        try {
            Long userId = checkParticipantAndGetId(authentication); // Throws if not participant
            logger.debug("Fetching messages for participant user ID: {}", userId);
            List<GlobalChatMessageDto> messages = globalChatService.getMessages();
            logger.info("Successfully fetched {} global chat messages.", messages.size());
            return ResponseEntity.ok(messages);
        } catch (UnauthorizedActionException e) {
            logger.warn("Unauthorized attempt to fetch global chat messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            logger.error("Error fetching global chat messages: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handles media uploads for the global chat.
     * Requires the user to be a participant.
     */
    @PostMapping(value = "/media", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadChatMedia(
            Authentication authentication,
            @RequestParam("media") MultipartFile mediaFile) {
        logger.info("Received request to upload media for global chat.");

        if (mediaFile == null || mediaFile.isEmpty()) {
            logger.warn("Media upload request rejected: File is missing.");
            return ResponseEntity.badRequest().body(Map.of("message", "Media file is required."));
        }

        Long userId;
        try {
            userId = checkParticipantAndGetId(authentication); // Throws if not participant
            logger.debug("Media upload initiated by participant user ID: {}", userId);
        } catch (UnauthorizedActionException e) {
            logger.warn("Unauthorized media upload attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Error checking participation for media upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Could not verify user participation."));
        }

        // Determine Media Type
        MediaType mediaType;
        String contentType = mediaFile.getContentType();
        logger.debug("Uploaded media content type: {}", contentType);
        if (contentType == null) {
            mediaType = null;
        } else if (contentType.startsWith("image")) {
            mediaType = MediaType.IMAGE;
        } else if (contentType.startsWith("video")) {
            mediaType = MediaType.VIDEO;
        } else if (contentType.startsWith("audio")) {
            mediaType = MediaType.AUDIO;
        } else {
            mediaType = null;
            logger.warn("Unsupported media type '{}' uploaded for global chat.", contentType);
            // Optionally return bad request if type is strictly unsupported
            // return ResponseEntity.badRequest().body(Map.of("message", "Unsupported media type: " + contentType));
        }
        logger.debug("Determined media type: {}", mediaType);

        // Store the file
        try {
            logger.debug("Attempting to store media file...");
            String fileName = localStorageService.storeFile(mediaFile);
            String fileAccessUrl = "/api/uploads/" + fileName; // Relative URL for client access

            logger.info("Stored global chat media file '{}' by user ID {}. Access URL: {}", fileName, userId, fileAccessUrl);

            return ResponseEntity.ok(Map.of(
                    "mediaUrl", fileAccessUrl,
                    "mediaType", mediaType != null ? mediaType.name() : "UNKNOWN"
            ));
        } catch (StorageException e) {
            logger.error("StorageException during global chat media upload by user ID {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during global chat media upload by user ID {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An internal error occurred while storing the media file."));
        }
    }

    /**
     * One-time-use endpoint for SUPER_ADMIN to add all existing users to the global chat.
     * Does NOT require the admin to be a participant already.
     */
    @PostMapping("/migrate-users")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')") // Secure this endpoint
    public ResponseEntity<?> migrateAllUsers(Authentication authentication) {
        try {
            // Get admin ID for logging, no participation check needed here
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long adminId = credentialsRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Admin user not found"))
                    .getUser().getId();

            logger.warn("SUPER_ADMIN (ID: {}) triggered migration of all users to global chat.", adminId);
            Map<String, Integer> result = globalChatService.migrateExistingUsersToGlobalChat();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to migrate users to global chat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Migration failed: " + e.getMessage()));
        }
    }
}