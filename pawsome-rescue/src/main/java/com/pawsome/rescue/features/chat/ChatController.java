package com.pawsome.rescue.features.chat;

import com.pawsome.rescue.features.chat.dto.MessageDto;
import com.pawsome.rescue.features.chat.dto.ParticipantDto;
import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
import com.pawsome.rescue.features.chat.entity.Message; // Import Message
import com.pawsome.rescue.features.chat.repository.ParticipantRepository;
import com.pawsome.rescue.features.storage.LocalStorageService; // Import Storage
import com.pawsome.rescue.features.storage.exception.StorageException; // Import StorageException
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Import MultipartFile

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    // --- ADD LOGGER AND STORAGE SERVICE ---
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    @Autowired
    private LocalStorageService localStorageService;
    // --- END ADD ---

    @Autowired
    private ChatService chatService;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @GetMapping("/groups")
    public ResponseEntity<List<ParticipantDto>> getChatGroupsForUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Long userId = credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        List<ParticipantDto> participantDtos = chatService.getParticipantDtosForUser(userId);

        return ResponseEntity.ok(participantDtos);
    }

    @GetMapping("/messages/{chatId}")
    public ResponseEntity<List<MessageDto>> getChatMessages(@PathVariable String chatId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Long userId = credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        boolean isParticipant = participantRepository.findByUserId(userId).stream()
                .anyMatch(p -> p.getChatGroup().getId().equals(chatId));

        if (!isParticipant) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<MessageDto> messages = chatService.getMessagesWithDetailsByChatGroupId(chatId);
        return ResponseEntity.ok(messages);
    }

    // --- NEW MEDIA UPLOAD ENDPOINT ---
    @PostMapping(value = "/{chatId}/media", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadChatMedia(
            Authentication authentication,
            @PathVariable String chatId,
            @RequestParam("media") MultipartFile mediaFile) {

        logger.info("Received media upload request for chat ID: {}", chatId);

        if (mediaFile == null || mediaFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Media file is required."));
        }

        // 1. Check if user is participant
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = credentialsRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();
        boolean isParticipant = participantRepository.findByUserId(userId).stream()
                .anyMatch(p -> p.getChatGroup().getId().equals(chatId));

        if (!isParticipant) {
            logger.warn("User ID {} attempted to upload media to chat ID {} but is not a participant.", userId, chatId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You are not a participant of this chat."));
        }

        // 2. Determine Media Type
        Message.MediaType mediaType;
        String contentType = mediaFile.getContentType();
        logger.debug("Uploaded media content type: {}", contentType);
        if (contentType == null) {
            mediaType = null;
        } else if (contentType.startsWith("image")) {
            mediaType = Message.MediaType.IMAGE;
        } else if (contentType.startsWith("video")) {
            mediaType = Message.MediaType.VIDEO;
        } else if (contentType.startsWith("audio")) {
            mediaType = Message.MediaType.AUDIO;
        } else {
            mediaType = null;
            logger.warn("Unsupported media type '{}' uploaded for chat ID {}.", contentType, chatId);
        }

        // 3. Store the file
        try {
            String fileName = localStorageService.storeFile(mediaFile);
            String fileAccessUrl = "/api/uploads/" + fileName; // URL for client access

            logger.info("Stored chat media file '{}' for chat ID {} by user ID {}. Access URL: {}", fileName, chatId, userId, fileAccessUrl);

            return ResponseEntity.ok(Map.of(
                    "mediaUrl", fileAccessUrl,
                    "mediaType", mediaType != null ? mediaType.name() : "UNKNOWN"
            ));
        } catch (StorageException e) {
            logger.error("StorageException during chat media upload for chat ID {}: {}", chatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during chat media upload for chat ID {}: {}", chatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An internal error occurred."));
        }
    }
    // --- END NEW MEDIA UPLOAD ENDPOINT ---

    @PostMapping("/groups/{chatId}/add-user")
    public ResponseEntity<?> addUserToGroup(@PathVariable String chatId, @RequestBody Map<String, String> body) {
        String usernameToAdd = body.get("username");
        User userToAdd = credentialsRepository.findByUsername(usernameToAdd)
                .orElseThrow(() -> new RuntimeException("User to add not found.")).getUser();

        try {
            chatService.addUserToChatGroup(chatId, userToAdd.getId());
            return ResponseEntity.ok("User added to chat group successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}