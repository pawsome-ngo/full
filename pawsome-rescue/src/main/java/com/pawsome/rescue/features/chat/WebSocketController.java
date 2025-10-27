package com.pawsome.rescue.features.chat;

import com.pawsome.rescue.features.chat.dto.MessageDto;
import com.pawsome.rescue.auth.repository.CredentialsRepository;
// --- ADD IMPORTS ---
import com.pawsome.rescue.features.chat.entity.Message.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// --- END IMPORTS ---
import com.pawsome.rescue.features.chat.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class WebSocketController {

    // --- ADD LOGGER ---
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    // --- END ADD ---

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @MessageMapping("/chat/{chatId}/send")
    public void sendMessageToGroup(Authentication authentication, @DestinationVariable String chatId, @Payload Map<String, String> payload) {
        logger.info("Received WebSocket message payload for /chat/{}/send: {}", chatId, payload); // Log payload

        String username = authentication.getName();
        Long senderId = credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Sender not found")).getUser().getId();
        logger.debug("Extracted sender ID: {}", senderId);

        boolean isParticipant = participantRepository.findByUserId(senderId).stream()
                .anyMatch(p -> p.getChatGroup().getId().equals(chatId));
        logger.debug("User {} participation check for chat {}: {}", senderId, chatId, isParticipant);

        if (isParticipant) {
            // --- EXTRACT NEW FIELDS ---
            String text = payload.get("text");
            String clientMessageId = payload.get("clientMessageId");
            String parentMessageId = payload.get("parentMessageId");
            String mediaUrl = payload.get("mediaUrl");
            String mediaTypeString = payload.get("mediaType");

            MediaType mediaType = null;
            if (mediaTypeString != null && !mediaTypeString.isBlank() && !mediaTypeString.equalsIgnoreCase("UNKNOWN")) {
                try {
                    mediaType = MediaType.valueOf(mediaTypeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid mediaType '{}' received for chat ID {}.", mediaTypeString, chatId);
                    mediaUrl = null; // Invalidate media if type is bad
                }
            }
            // --- END EXTRACTION ---

            try {
                // --- CALL UPDATED SERVICE ---
                MessageDto messageDto = chatService.saveMessage(chatId, senderId, text, clientMessageId, parentMessageId, mediaUrl, mediaType);
                if (messageDto != null) {
                    logger.info("Message saved (ID: {}). Broadcasting to /topic/chat/{}", messageDto.getId(), chatId);
                    messagingTemplate.convertAndSend("/topic/chat/" + chatId, messageDto);
                } else {
                    logger.error("ChatService returned null DTO for message save (Chat: {}, User: {})", chatId, senderId);
                }
            } catch (Exception e) {
                logger.error("Error saving message via WebSocket for chat ID {} from user ID {}: {}", chatId, senderId, e.getMessage(), e);
            }
        } else {
            logger.warn("User ID {} (username: {}) attempted to send message to chat ID {} but is not a participant.", senderId, username, chatId);
        }
    }

    @MessageMapping("/chat/{chatId}/react")
    public void addReactionToMessage(Authentication authentication, @DestinationVariable String chatId, @Payload Map<String, String> payload) {
        logger.info("Received WebSocket message payload for /chat/{}/react: {}", chatId, payload);
        String username = authentication.getName();
        Long userId = credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();

        String messageId = payload.get("messageId");
        String reaction = payload.get("reaction");

        if (messageId == null || reaction == null) {
            logger.warn("Missing messageId or reaction in payload for /chat/{}/react", chatId);
            return;
        }

        try {
            MessageDto messageDto = chatService.addReaction(messageId, userId, reaction);
            if (messageDto != null) {
                logger.info("Reaction added. Broadcasting update for message ID {} to /topic/chat/{}", messageId, chatId);
                messagingTemplate.convertAndSend("/topic/chat/" + chatId, messageDto);
            } else {
                logger.error("ChatService returned null DTO for reaction add (Message: {}, User: {})", messageId, userId);
            }
        } catch (Exception e) {
            logger.error("Error adding reaction via WebSocket for message ID {} by user ID {}: {}", messageId, userId, e.getMessage(), e);
        }
    }

    @MessageMapping("/chat/{chatId}/read")
    public void markMessageAsRead(Authentication authentication, @DestinationVariable String chatId, @Payload Map<String, String> payload) {
        logger.info("Received WebSocket message payload for /chat/{}/read: {}", chatId, payload);
        String username = authentication.getName();
        Long userId = credentialsRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")).getUser().getId();
        String messageId = payload.get("messageId");

        if (messageId == null) {
            logger.warn("Missing messageId in payload for /chat/{}/read", chatId);
            return;
        }

        try {
            MessageDto updatedMessageDto = chatService.markAsReadAndGetUpdatedMessage(messageId, userId);
            logger.info("Read receipt processed for message ID {}. Broadcasting update to /topic/chat/{}", messageId, chatId);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, updatedMessageDto);
        } catch (Exception e) {
            logger.error("Error processing read receipt via WebSocket for message ID {} by user ID {}: {}", messageId, userId, e.getMessage(), e);
        }
    }
}