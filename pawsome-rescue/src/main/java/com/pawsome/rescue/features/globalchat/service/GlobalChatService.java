package com.pawsome.rescue.features.globalchat.service;

import com.pawsome.rescue.auth.dto.UserDto;
import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.UserRepository;
import com.pawsome.rescue.features.globalchat.dto.GlobalChatMessageDto;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatMessage;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatMessage.MediaType;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatParticipant;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatReaction;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatReadReceipt;
import com.pawsome.rescue.features.globalchat.repository.GlobalChatMessageRepository;
import com.pawsome.rescue.features.globalchat.repository.GlobalChatParticipantRepository;
import com.pawsome.rescue.features.globalchat.repository.GlobalChatReactionRepository;
import com.pawsome.rescue.features.globalchat.repository.GlobalChatReadReceiptRepository;
import com.pawsome.rescue.features.notification.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList; // Added import
import java.util.List;
import java.util.Map;
import java.util.Set; // Added import
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GlobalChatService {

    private static final Logger logger = LoggerFactory.getLogger(GlobalChatService.class);

    @Autowired private GlobalChatMessageRepository messageRepository;
    @Autowired private GlobalChatParticipantRepository participantRepository;
    @Autowired private GlobalChatReactionRepository reactionRepository;
    @Autowired private GlobalChatReadReceiptRepository readReceiptRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PushNotificationService pushNotificationService;

    /**
     * Adds a newly authorized user to the global chat.
     */
    @Transactional
    public void addUserToGlobalChat(User user) {
        logger.debug("Attempting to add user ID {} to global chat.", user.getId());
        if (user == null || user.getId() == null) {
            logger.error("addUserToGlobalChat: Attempted to add null user.");
            return;
        }
        if (participantRepository.existsById(user.getId())) {
            logger.warn("addUserToGlobalChat: User {} is already in the global chat.", user.getId());
            return;
        }
        GlobalChatParticipant participant = new GlobalChatParticipant();
        participant.setUser(user);
        // participant.setUserId(user.getId()); // Let @MapsId handle this
        participant.setJoinedAt(LocalDateTime.now());
        participantRepository.save(participant);
        logger.info("addUserToGlobalChat: User {} ({}) has been added.", user.getId(), user.getFirstName());
    }

    /**
     * Fetches all messages for the global chat.
     */
    @Transactional(readOnly = true)
    public List<GlobalChatMessageDto> getMessages() {
        logger.debug("Fetching all global chat messages...");
        List<GlobalChatMessage> messages = messageRepository.findAllByOrderByTimestampAsc();
        logger.debug("Found {} messages.", messages.size());
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Saves a new message and notifies all other participants.
     */
    @Transactional
    public GlobalChatMessageDto saveMessage(Long senderId, String text, String clientMessageId, String parentMessageId, String mediaUrl, MediaType mediaType) {
        logger.info("saveMessage called - SenderID: {}, Text: '{}', ClientID: '{}', ParentID: '{}', MediaURL: '{}', MediaType: {}",
                senderId, text != null ? text.substring(0, Math.min(text.length(), 20))+"..." : "null", clientMessageId, parentMessageId, mediaUrl, mediaType); // Log input

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender user not found with ID: " + senderId));

        if ((text == null || text.isBlank()) && mediaUrl == null) {
            logger.warn("saveMessage: Attempted to save empty message (no text or media) for user ID {}", senderId);
            throw new IllegalArgumentException("Cannot save an empty message (must have text or media).");
        }

        GlobalChatMessage message = new GlobalChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setSender(sender);
        message.setText((text != null && !text.isBlank()) ? text.trim() : null);
        message.setClientMessageId(clientMessageId);
        message.setParentMessageId(parentMessageId);
        message.setTimestamp(LocalDateTime.now());
        message.setMediaUrl(mediaUrl);
        message.setMediaType(mediaType);

        logger.debug("Attempting to save global chat message entity...");
        GlobalChatMessage savedMessage = messageRepository.save(message);
        logger.info("Global chat message saved successfully with ID: {}", savedMessage.getId());

//        // --- Notification Logic ---
//        String notificationText = text;
//        if (mediaType != null && (notificationText == null || notificationText.isBlank())) {
//            notificationText = "[" + mediaType.name().toLowerCase() + "]";
//        } else if (mediaType != null && notificationText != null) {
//            notificationText = text + " [" + mediaType.name().toLowerCase() + "]";
//        }
//
//        if (notificationText != null && !notificationText.isBlank()) {
//            logger.debug("Preparing push notifications for message ID: {}", savedMessage.getId());
//            List<GlobalChatParticipant> allParticipants = participantRepository.findAll();
//            logger.debug("Found {} total participants for notifications.", allParticipants.size());
//            String pushPayload = String.format("@%s (Global Chat): %s",
//                    sender.getFirstName(),
//                    notificationText
//            );
//
//            int notifiedCount = 0;
//            for (GlobalChatParticipant participant : allParticipants) {
//                if (!participant.getUserId().equals(senderId) && participant.getUser() != null) {
//                    try {
//                        pushNotificationService.sendNotificationToUser(participant.getUser(), pushPayload);
//                        notifiedCount++;
//                    } catch (Exception e) {
//                        logger.error("Failed to send Global Chat PUSH notification to user ID {}: {}", participant.getUserId(), e.getMessage());
//                        // Consider logging only, maybe remove subscription if error persists
//                    }
//                }
//            }
//            logger.info("Attempted to send push notifications to {} participants for message ID {}.", notifiedCount, savedMessage.getId());
//        }
//         --- End Notification Logic ---

        return convertToDto(savedMessage);
    }

    /**
     * Adds or updates a reaction to a message.
     */
    @Transactional
    public GlobalChatMessageDto addReaction(String messageId, Long userId, String reaction) {
        logger.info("addReaction called - MessageID: {}, UserID: {}, Reaction: '{}'", messageId, userId, reaction);
        GlobalChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Global chat message not found: " + messageId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Remove existing reaction by this user on this message, if any
        reactionRepository.findByMessageAndUser(message, user)
                .ifPresent(existingReaction -> {
                    logger.debug("Removing existing reaction by user {} on message {}", userId, messageId);
                    reactionRepository.delete(existingReaction);
                });

        GlobalChatReaction newReaction = new GlobalChatReaction();
        newReaction.setMessage(message);
        newReaction.setUser(user);
        newReaction.setReaction(reaction);
        reactionRepository.save(newReaction);
        logger.debug("Saved new reaction by user {} to message {}", userId, messageId);

        GlobalChatMessage updatedMessage = messageRepository.findById(messageId).orElse(message);
        return convertToDto(updatedMessage);
    }

    /**
     * Marks a message as read by a user.
     */
    @Transactional
    public GlobalChatMessageDto markAsRead(String messageId, Long userId) {
        logger.info("markAsRead called - MessageID: {}, UserID: {}", messageId, userId);
        GlobalChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Global chat message not found: " + messageId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (readReceiptRepository.findByMessageIdAndUserId(messageId, userId).isEmpty()) {
            GlobalChatReadReceipt newReceipt = new GlobalChatReadReceipt();
            newReceipt.setMessage(message);
            newReceipt.setUser(user);
            readReceiptRepository.save(newReceipt);
            logger.debug("Saved new read receipt for message ID {} by user ID {}", messageId, userId);

            GlobalChatMessage updatedMessage = messageRepository.findById(messageId).orElse(message);
            return convertToDto(updatedMessage);
        } else {
            logger.trace("Read receipt already exists for message ID {} by user ID {}", messageId, userId);
            return convertToDto(message); // Return current state if no change
        }
    }

    /**
     * Converts the entity to a DTO, resolving related data.
     */
    @Transactional(readOnly = true)
    public GlobalChatMessageDto convertToDto(GlobalChatMessage message) {
        // Basic null check
        if (message == null) {
            logger.warn("convertToDto called with null message entity.");
            return null;
        }
        logger.trace("Converting message ID {} to DTO...", message.getId());

        GlobalChatMessageDto dto = new GlobalChatMessageDto();
        dto.setId(message.getId());
        dto.setText(message.getText());
        dto.setTimestamp(message.getTimestamp());
        dto.setClientMessageId(message.getClientMessageId());
        dto.setParentMessageId(message.getParentMessageId());
        dto.setMediaUrl(message.getMediaUrl());
        dto.setMediaType(message.getMediaType());

        // Sender
        UserDto senderDto = new UserDto();
        if (message.getSender() != null) {
            senderDto.setId(message.getSender().getId());
            senderDto.setFirstName(message.getSender().getFirstName());
            senderDto.setLastName(message.getSender().getLastName());
        } else {
            logger.warn("Message ID {} has a null sender during DTO conversion.", message.getId());
        }
        dto.setSender(senderDto);

        // Reactions (Fetch related data within transaction)
        List<GlobalChatReaction> reactions = reactionRepository.findByMessageId(message.getId());
        logger.trace("Found {} reactions for message ID {}", reactions.size(), message.getId());
        dto.setReactions(reactions.stream()
                .filter(r -> r.getUser() != null)
                .collect(Collectors.groupingBy(GlobalChatReaction::getReaction, Collectors.mapping(
                        r -> new UserDto(r.getUser().getId(), r.getUser().getFirstName(), r.getUser().getLastName()),
                        Collectors.toList()
                ))));

        // Read Receipts (Fetch related data within transaction)
        List<GlobalChatReadReceipt> receipts = readReceiptRepository.findByMessageId(message.getId());
        logger.trace("Found {} read receipts for message ID {}", receipts.size(), message.getId());
        dto.setSeenBy(receipts.stream()
                .filter(r -> r.getUser() != null)
                .map(r -> new UserDto(r.getUser().getId(), r.getUser().getFirstName(), r.getUser().getLastName()))
                .collect(Collectors.toList()));

        logger.trace("Finished converting message ID {} to DTO.", message.getId());
        return dto;
    }

    /**
     * One-time migration to add all existing authorized users to the global chat.
     * @return A map summarizing the migration.
     */
    @Transactional
    public Map<String, Integer> migrateExistingUsersToGlobalChat() {
        logger.info("Starting migration of existing users to global chat...");
        List<User> allUsers = userRepository.findAll(); // Find all registered users
        logger.debug("Found {} total registered users.", allUsers.size());

        Set<Long> existingParticipantIds = participantRepository.findAll().stream()
                .map(GlobalChatParticipant::getUserId)
                .collect(Collectors.toSet());
        logger.debug("Found {} existing participants in global chat.", existingParticipantIds.size());

        int usersAdded = 0;
        int usersSkipped = 0;

        List<GlobalChatParticipant> newParticipants = new ArrayList<>();

        for (User user : allUsers) {
            // Check if the user is already in the participant set
            if (!existingParticipantIds.contains(user.getId())) {
                logger.trace("User ID {} is not yet a participant. Creating record...", user.getId());
                GlobalChatParticipant participant = new GlobalChatParticipant();
                participant.setUser(user); // Set the User association
                // DO NOT set participant.setUserId(user.getId()); <-- Let @MapsId handle this
                participant.setJoinedAt(LocalDateTime.now());
                newParticipants.add(participant);
                usersAdded++;
            } else {
                logger.trace("User ID {} is already a participant. Skipping...", user.getId());
                usersSkipped++;
            }
        }

        // Save all the newly created participant records in batch
        if (!newParticipants.isEmpty()) {
            logger.info("Saving {} new participant records...", newParticipants.size());
            participantRepository.saveAll(newParticipants);
        } else {
            logger.info("No new users to add to the global chat.");
        }

        logger.info("Migration complete. {} users added, {} users already existed.", usersAdded, usersSkipped);
        return Map.of("usersAdded", usersAdded, "usersSkipped", usersSkipped);
    }
}