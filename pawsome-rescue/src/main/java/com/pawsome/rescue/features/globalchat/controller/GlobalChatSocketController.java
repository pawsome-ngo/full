package com.pawsome.rescue.features.globalchat.controller;

import com.pawsome.rescue.auth.repository.CredentialsRepository;
import com.pawsome.rescue.features.globalchat.dto.GlobalChatMessageDto;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatMessage.MediaType;
import com.pawsome.rescue.features.globalchat.repository.GlobalChatParticipantRepository;
import com.pawsome.rescue.features.globalchat.service.GlobalChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class GlobalChatSocketController {

    private static final Logger logger = LoggerFactory.getLogger(GlobalChatSocketController.class);
    private static final String BROADCAST_DESTINATION = "/topic/gchat"; // Global topic

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private GlobalChatService globalChatService;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private GlobalChatParticipantRepository participantRepository;

    // Helper to get user ID
    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User credentials not found for: " + username))
                .getUser().getId();
    }

    // Helper to check participation
    private boolean isParticipant(Long userId) {
        boolean participant = participantRepository.existsById(userId);
        logger.trace("Checking participation for user ID {}: {}", userId, participant); // Use TRACE for frequent checks
        return participant;
    }

    @MessageMapping("/gchat/send") // Global chat send endpoint
    public void sendMessage(Authentication authentication, @Payload Map<String, String> payload) {
        logger.info("Received WebSocket message payload for /gchat/send: {}", payload); // Log incoming payload

        if (authentication == null || payload == null) {
            logger.error("Received null authentication or payload for /gchat/send");
            return;
        }

        Long senderId;
        try {
            senderId = getUserId(authentication);
            logger.debug("Extracted sender ID: {}", senderId);
        } catch (RuntimeException e) {
            logger.error("Error retrieving sender ID for global chat send: {}", e.getMessage());
            return;
        }

        if (!isParticipant(senderId)) {
            logger.warn("User ID {} (not participant) tried to send message to /gchat/send", senderId);
            // Cannot easily send error back here, just drop the message.
            return;
        }

        // --- Extract fields from payload ---
        String text = payload.get("text");
        String clientMessageId = payload.get("clientMessageId");
        String parentMessageId = payload.get("parentMessageId");
        String mediaUrl = payload.get("mediaUrl");
        String mediaTypeString = payload.get("mediaType");

        logger.debug("Extracted fields - Text: '{}', ClientID: '{}', ParentID: '{}', MediaURL: '{}', MediaTypeString: '{}'",
                text, clientMessageId, parentMessageId, mediaUrl, mediaTypeString);

        MediaType mediaType = null;
        if (mediaTypeString != null && !mediaTypeString.isBlank() && !mediaTypeString.equalsIgnoreCase("UNKNOWN")) {
            try {
                mediaType = MediaType.valueOf(mediaTypeString.toUpperCase());
                logger.debug("Parsed MediaType: {}", mediaType);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid mediaType '{}' received in WebSocket payload for global chat. Ignoring media part.", mediaTypeString);
                mediaUrl = null; // Ignore URL if type is invalid
            }
        }

        // Validate: must have text or valid media
        if ((text == null || text.isBlank()) && mediaUrl == null) {
            logger.warn("Ignoring empty message (no text or valid media) received via WebSocket for global chat from user ID {}", senderId);
            return; // Don't save or broadcast
        }

        // --- Call service method ---
        try {
            logger.debug("Calling globalChatService.saveMessage for sender ID {}", senderId);
            GlobalChatMessageDto messageDto = globalChatService.saveMessage(senderId, text, clientMessageId, parentMessageId, mediaUrl, mediaType);

            if (messageDto != null) {
                logger.info("Message saved successfully (ID: {}). Broadcasting to {}", messageDto.getId(), BROADCAST_DESTINATION);
                messagingTemplate.convertAndSend(BROADCAST_DESTINATION, messageDto);
            } else {
                logger.error("globalChatService.saveMessage returned null DTO for user ID {}", senderId);
            }
        } catch (IllegalArgumentException e) { // Catch validation errors from service
            logger.error("Validation error saving global chat message from user ID {}: {}", senderId, e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            logger.error("Unexpected error saving global chat message from user ID {}: {}", senderId, e.getMessage(), e);
        }
    }

    @MessageMapping("/gchat/react") // Global chat reaction endpoint
    public void addReaction(Authentication authentication, @Payload Map<String, String> payload) {
        logger.info("Received WebSocket message payload for /gchat/react: {}", payload);
        if (authentication == null || payload == null) return;

        Long userId;
        try { userId = getUserId(authentication); }
        catch (RuntimeException e) { logger.error("Error retrieving user ID for reaction: {}", e.getMessage()); return; }

        if (!isParticipant(userId)) {
            logger.warn("User ID {} (not participant) tried to send reaction to /gchat/react", userId);
            return;
        }

        String messageId = payload.get("messageId");
        String reaction = payload.get("reaction");
        logger.debug("Extracted fields - MessageID: '{}', Reaction: '{}', UserID: {}", messageId, reaction, userId);

        if (messageId == null || reaction == null) {
            logger.warn("Missing messageId or reaction in payload for /gchat/react");
            return;
        }

        try {
            logger.debug("Calling globalChatService.addReaction...");
            GlobalChatMessageDto messageDto = globalChatService.addReaction(messageId, userId, reaction);
            if (messageDto != null) {
                logger.info("Reaction added/updated successfully. Broadcasting update for message ID {} to {}", messageId, BROADCAST_DESTINATION);
                messagingTemplate.convertAndSend(BROADCAST_DESTINATION, messageDto);
            } else {
                logger.error("globalChatService.addReaction returned null DTO for message ID {}, user ID {}", messageId, userId);
            }
        } catch (Exception e) {
            logger.error("Error adding global chat reaction from user ID {} for message ID {}: {}", userId, messageId, e.getMessage(), e);
        }
    }

    @MessageMapping("/gchat/read") // Global chat read receipt endpoint
    public void markAsRead(Authentication authentication, @Payload Map<String, String> payload) {
        logger.info("Received WebSocket message payload for /gchat/read: {}", payload);
        if (authentication == null || payload == null) return;

        Long userId;
        try { userId = getUserId(authentication); }
        catch (RuntimeException e) { logger.error("Error retrieving user ID for read receipt: {}", e.getMessage()); return; }

        if (!isParticipant(userId)) {
            logger.warn("User ID {} (not participant) tried to send read receipt to /gchat/read", userId);
            return;
        }

        String messageId = payload.get("messageId");
        logger.debug("Extracted fields - MessageID: '{}', UserID: {}", messageId, userId);

        if (messageId == null) {
            logger.warn("Missing messageId in payload for /gchat/read");
            return;
        }

        try {
            logger.debug("Calling globalChatService.markAsRead...");
            GlobalChatMessageDto updatedMessageDto = globalChatService.markAsRead(messageId, userId);
            // Always broadcast the update, even if the receipt already existed
            logger.info("Read receipt processed for message ID {}. Broadcasting update to {}", messageId, BROADCAST_DESTINATION);
            messagingTemplate.convertAndSend(BROADCAST_DESTINATION, updatedMessageDto);
        } catch (Exception e) {
            logger.error("Error processing global chat read receipt from user ID {} for message ID {}: {}", userId, messageId, e.getMessage(), e);
        }
    }
}