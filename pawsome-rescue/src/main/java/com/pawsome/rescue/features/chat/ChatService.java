package com.pawsome.rescue.features.chat;

import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.auth.repository.UserRepository;
import com.pawsome.rescue.features.chat.dto.ChatGroupDto;
import com.pawsome.rescue.features.chat.dto.MessageDto;
import com.pawsome.rescue.auth.dto.UserDto;
import com.pawsome.rescue.features.chat.dto.ParticipantDto;
import com.pawsome.rescue.features.chat.entity.*;
// --- ADD IMPORT ---
import com.pawsome.rescue.features.chat.entity.Message.MediaType;
// --- END IMPORT ---
import com.pawsome.rescue.features.chat.repository.*;
import com.pawsome.rescue.features.notification.PushNotificationService;
import com.pawsome.rescue.features.storage.LocalStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pawsome.rescue.features.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime; // Added import
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatGroupRepository chatGroupRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private ReadReceiptRepository readReceiptRepository;

    // Add this if it's not already present in ChatService.java
    @Autowired
    private LocalStorageService localStorageService;

    @Autowired private NotificationService notificationService;
    @Autowired private PushNotificationService pushNotificationService;

    public List<Participant> getChatGroupsByUserId(Long userId) {
        return participantRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ParticipantDto> getParticipantDtosForUser(Long userId) {
        List<Participant> participants = participantRepository.findByUserId(userId);

        return participants.stream()
                .map(participant -> {
                    ParticipantDto dto = new ParticipantDto();
                    ChatGroup chatGroup = participant.getChatGroup();

                    ChatGroupDto chatGroupDto = new ChatGroupDto();
                    chatGroupDto.setId(chatGroup.getId());
                    chatGroupDto.setName(chatGroup.getName());
                    chatGroupDto.setPurposeId(chatGroup.getPurposeId());
                    dto.setChatGroup(chatGroupDto);

                    Message lastMessage = messageRepository.findTopByChatGroupIdOrderByTimestampDesc(chatGroup.getId());

                    if (lastMessage != null) {
                        // --- MODIFIED to show media placeholders ---
                        if (lastMessage.getMediaUrl() != null && (lastMessage.getText() == null || lastMessage.getText().isBlank())) {
                            dto.setLastMessage("[" + lastMessage.getMediaType().name().toLowerCase() + "]");
                        } else if (lastMessage.getMediaUrl() != null && lastMessage.getText() != null) {
                            dto.setLastMessage(lastMessage.getText() + " [" + lastMessage.getMediaType().name().toLowerCase() + "]");
                        } else {
                            dto.setLastMessage(lastMessage.getText());
                        }
                        // --- END MODIFICATION ---
                        dto.setLastMessageTimestamp(lastMessage.getTimestamp());

                        boolean isUnread = readReceiptRepository.findByMessageIdAndUserId(lastMessage.getId(), userId).isEmpty()
                                && !lastMessage.getSender().getId().equals(userId);
                        dto.setHasUnreadMessages(isUnread);
                    }

                    return dto;
                })
                .sorted(Comparator.comparing(ParticipantDto::getLastMessageTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<Message> getMessagesByChatGroupId(String chatId) {
        return messageRepository.findByChatGroup_IdOrderByTimestampAsc(chatId);
    }

    @Transactional
    public List<MessageDto> getMessagesWithDetailsByChatGroupId(String chatId) {
        List<Message> messages = messageRepository.findByChatGroup_IdOrderByTimestampAsc(chatId);
        return messages.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public MessageDto saveMessage(String chatId, Long senderId, String text, String clientMessageId, String parentMessageId, String mediaUrl, MediaType mediaType) {
        Optional<ChatGroup> chatGroupOptional = chatGroupRepository.findById(chatId);
        Optional<User> senderOptional = userRepository.findById(senderId);

        if (chatGroupOptional.isPresent() && senderOptional.isPresent()) {
            ChatGroup chatGroup = chatGroupOptional.get();
            User sender = senderOptional.get();

            // --- VALIDATION ---
            if ((text == null || text.isBlank()) && mediaUrl == null) {
                logger.warn("Attempted to save empty message (no text or media) for chat ID {} by user ID {}", chatId, senderId);
                throw new IllegalArgumentException("Cannot save an empty message (must have text or media).");
            }
            // --- END VALIDATION ---

            Message message = new Message();
            message.setId(UUID.randomUUID().toString());
            message.setChatGroup(chatGroup);
            message.setSender(sender);
            String originalText = (text != null) ? text : "";
            message.setText((!originalText.isBlank()) ? originalText.trim() : null);
            message.setClientMessageId(clientMessageId);
            message.setParentMessageId(parentMessageId);
            message.setTimestamp(LocalDateTime.now());
            message.setMediaUrl(mediaUrl);
            message.setMediaType(mediaType);

            Message savedMessage = messageRepository.save(message);
            logger.info("Saved message ID {} for chat group ID {} from user ID {}", savedMessage.getId(), chatId, senderId);

            // --- REVISED Notification Logic (Handles Punctuation for @Everyone) ---
            if (!originalText.isBlank()) {
                Set<User> usersToNotify = new HashSet<>();
                List<Participant> allParticipants = participantRepository.findByChatGroupId(chatId);

                // Updated Regex: Check for @Everyone (case-insensitive) OR
                // @ followed by common punctuation (?, !, .) and then space/end OR
                // @ followed by space/end
                Pattern everyonePattern = Pattern.compile(
                        "@Everyone|@([?!.]+)(?=\\s|$)|@(?=\\s|$)", // Punctuation captured but not used, just detected
                        Pattern.CASE_INSENSITIVE
                );
                Matcher everyoneMatcher = everyonePattern.matcher(originalText);
                boolean mentionEveryoneEquivalent = everyoneMatcher.find();

                if (mentionEveryoneEquivalent) {
                    // Add all participants except the sender
                    for (Participant participant : allParticipants) {
                        if (participant != null && participant.getUser() != null && !participant.getUser().getId().equals(senderId)) {
                            usersToNotify.add(participant.getUser());
                        }
                    }
                    logger.info("Message contains @Everyone equivalent, targeting all participants in chat group ID {}", chatId);

                } else {
                    // Find specific mentions if @Everyone equivalent is not present
                    Pattern mentionPattern = Pattern.compile("@([\\w]+)"); // Capture only word characters after @
                    Matcher matcher = mentionPattern.matcher(originalText);
                    Set<String> mentionedFirstNamesLower = new HashSet<>();

                    while (matcher.find()) {
                        String potentialName = matcher.group(1);
                        if (!"everyone".equalsIgnoreCase(potentialName)) { // Avoid matching "Everyone" here again
                            mentionedFirstNamesLower.add(potentialName.toLowerCase());
                        }
                    }

                    if (!mentionedFirstNamesLower.isEmpty()) {
                        logger.info("Found mentions in message for chat group ID {}: {}", chatId, mentionedFirstNamesLower);
                        for (Participant participant : allParticipants) {
                            User user = participant.getUser();
                            if (user != null && !user.getId().equals(senderId) &&
                                    user.getFirstName() != null &&
                                    mentionedFirstNamesLower.contains(user.getFirstName().toLowerCase()))
                            {
                                usersToNotify.add(user);
                            }
                        }
                    }
                }

                // Send notifications if there are users to notify
                if (!usersToNotify.isEmpty()) {
                    String notificationTextContent = originalText;
                    if (mediaType != null) {
                        notificationTextContent = originalText + " [" + mediaType.name().toLowerCase() + "]";
                    }

                    String pushPayload = String.format("@%s (%s): %s",
                            sender.getFirstName(),
                            chatGroup.getName(),
                            notificationTextContent
                    );

                    logger.info("Sending PUSH notifications for message in chat group ID {} to {} users.", chatId, usersToNotify.size());
                    for (User user : usersToNotify) {
                        if (user == null) {
                            logger.warn("Attempted to send notification to a null user in chat group {}", chatId);
                            continue;
                        }
                        try {
                            pushNotificationService.sendNotificationToUser(user, pushPayload);
                        } catch (Exception e) {
                            logger.error("Failed to send chat PUSH notification for message ID {} to user ID {}: {}",
                                    savedMessage.getId(), user.getId(), e.getMessage());
                        }
                    }
                } else {
                    logger.info("No specific users to notify via PUSH for message in chat group ID {}", chatId);
                }

            } else {
                logger.info("Message contains only media, no PUSH notifications sent based on text for chat group ID {}", chatId);
                // Optional: Add logic to notify everyone for media-only messages here
            }
            // --- End REVISED Notification Logic ---

            return convertToDto(savedMessage);
        }

        // Handle cases where chat group or sender wasn't found
        if (chatGroupOptional.isEmpty()) {
            logger.error("Failed to save message: Chat group ID {} not found.", chatId);
            throw new EntityNotFoundException("Chat group not found with ID: " + chatId);
        } else { // senderOptional must be empty
            logger.error("Failed to save message: Sender ID {} not found.", senderId);
            throw new EntityNotFoundException("Sender not found with ID: " + senderId);
        }
    }

    @Transactional
    public MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setText(message.getText());
        dto.setTimestamp(message.getTimestamp());
        dto.setClientMessageId(message.getClientMessageId());
        dto.setParentMessageId(message.getParentMessageId());

        // --- ADDED MAPPINGS ---
        dto.setMediaUrl(message.getMediaUrl());
        dto.setMediaType(message.getMediaType());
        // --- END MAPPINGS ---

        UserDto senderDto = new UserDto();
        if (message.getSender() != null) {
            senderDto.setId(message.getSender().getId());
            senderDto.setFirstName(message.getSender().getFirstName());
            senderDto.setLastName(message.getSender().getLastName());
        }
        dto.setSender(senderDto);

        List<Reaction> reactions = reactionRepository.findByMessageId(message.getId());
        Map<String, List<UserDto>> reactionsMap = reactions.stream()
                .filter(r -> r.getUser() != null) // Add null check
                .collect(Collectors.groupingBy(Reaction::getReaction, Collectors.mapping(
                        r -> new UserDto(r.getUser().getId(), r.getUser().getFirstName(), r.getUser().getLastName()),
                        Collectors.toList()
                )));
        dto.setReactions(reactionsMap);

        List<ReadReceipt> receipts = readReceiptRepository.findByMessageId(message.getId());
        List<UserDto> seenBy = receipts.stream()
                .filter(r -> r.getUser() != null) // Add null check
                .map(r -> new UserDto(r.getUser().getId(), r.getUser().getFirstName(), r.getUser().getLastName()))
                .collect(Collectors.toList());
        dto.setSeenBy(seenBy);

        return dto;
    }

    @Transactional
    public MessageDto addReaction(String messageId, Long userId, String reaction) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Remove existing reaction by same user first
        reactionRepository.findByMessageId(messageId).stream()
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(reactionRepository::delete);

        Reaction newReaction = new Reaction();
        newReaction.setMessage(message);
        newReaction.setUser(user);
        newReaction.setReaction(reaction);
        reactionRepository.save(newReaction);

        // Fetch message again to get updated reaction list
        Message updatedMessage = messageRepository.findById(messageId).orElse(message);
        return convertToDto(updatedMessage);
    }

    @Transactional
    public MessageDto markAsReadAndGetUpdatedMessage(String messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (readReceiptRepository.findByMessageIdAndUserId(messageId, userId).isEmpty()) {
            ReadReceipt newReceipt = new ReadReceipt();
            newReceipt.setMessage(message);
            newReceipt.setUser(user);
            readReceiptRepository.save(newReceipt);

            // Return DTO of the *updated* message
            Message updatedMessage = messageRepository.findById(messageId).orElse(message);
            return convertToDto(updatedMessage);
        }

        // No change, just return DTO of the message
        return convertToDto(message);
    }

    public Participant addUserToChatGroup(String chatId, Long userId) {
        Optional<ChatGroup> chatGroupOptional = chatGroupRepository.findById(chatId);
        Optional<User> userOptional = userRepository.findById(userId);

        if (chatGroupOptional.isPresent() && userOptional.isPresent()) {
            Participant participant = new Participant();
            Participant.ParticipantId participantId = new Participant.ParticipantId();
            participantId.setChatId(chatId);
            participantId.setUserId(userId);

            // Check if participant already exists
            if (participantRepository.existsById(participantId)) {
                logger.warn("User {} is already in chat group {}", userId, chatId);
                return participantRepository.findById(participantId).get(); // Return existing
            }

            participant.setId(participantId);
            participant.setChatGroup(chatGroupOptional.get());
            participant.setUser(userOptional.get());
            return participantRepository.save(participant);
        }
        throw new IllegalArgumentException("Chat group or user not found.");
    }

    @Transactional
    public ChatGroup createChatGroup(List<Long> userIds, String groupName, String purpose, Long purposeId) {
        List<User> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new IllegalArgumentException("One or more users not found.");
        }

        ChatGroup newGroup = new ChatGroup();
        newGroup.setId(UUID.randomUUID().toString());
        newGroup.setName(groupName);
        newGroup.setPurpose(purpose);
        newGroup.setPurposeId(purposeId);
        chatGroupRepository.save(newGroup);

        for (User user : users) {
            Participant participant = new Participant();
            Participant.ParticipantId participantId = new Participant.ParticipantId();
            participantId.setChatId(newGroup.getId());
            participantId.setUserId(user.getId());
            participant.setId(participantId);
            participant.setChatGroup(newGroup);
            participant.setUser(user);
            participantRepository.save(participant);
        }

        return newGroup;
    }

    @Transactional
    public void deleteChatGroupAndData(String chatGroupId) {
        if (chatGroupId == null || chatGroupId.isEmpty()) {
            return;
        }

        logger.warn("Deleting all data for chat group ID: {}", chatGroupId);

        List<Message> messagesToDelete = messageRepository.findByChatGroup_IdOrderByTimestampAsc(chatGroupId);

        // --- Step 2: Delete associated media files ---
        int mediaFilesDeleted = 0;
        int mediaFilesFailed = 0;
        for (Message message : messagesToDelete) {
            if (message.getMediaUrl() != null && !message.getMediaUrl().isBlank()) {
                try {
                    String relativePath = message.getMediaUrl();
                    // Assuming URL format /uploads/filename.ext
                    if (relativePath.startsWith("/uploads/")) {
                        String filename = relativePath.substring("/uploads/".length());
                        localStorageService.deleteFile(filename);
                    } else {
                        logger.warn("Media URL format not recognized for deletion for message ID {}: {}", message.getId(), relativePath);
                        mediaFilesFailed++;
                    }
                } catch (Exception e) {
                    logger.error("Error deleting media file for message ID {}: {}", message.getId(), e.getMessage());
                    mediaFilesFailed++;
                    // Continue deleting other data even if file deletion fails
                }
            }
        }
        if (!messagesToDelete.isEmpty()) {
            logger.info("Media file deletion attempt summary for chat group ID {}: {} deleted, {} failed/skipped.", chatGroupId, mediaFilesDeleted, mediaFilesFailed);
        }
        reactionRepository.deleteAllByChatGroupId(chatGroupId);
        readReceiptRepository.deleteAllByChatGroupId(chatGroupId);
        messageRepository.deleteAllByChatGroupId(chatGroupId);
        participantRepository.deleteAllByChatGroupId(chatGroupId);
        chatGroupRepository.deleteById(chatGroupId);
        logger.info("Successfully deleted chat group and data for ID: {}", chatGroupId);
    }

//    @Transactional
//    public void deleteChatGroupAndData(String chatId) {
//        Optional<ChatGroup> chatGroupOptional = chatGroupRepository.findById(chatId);
//        if (chatGroupOptional.isEmpty()) {
//            logger.warn("Attempted to delete non-existent chat group with ID: {}", chatId);
//            // Optionally throw an exception or just return
//            // throw new EntityNotFoundException("Chat group not found with ID: " + chatId);
//            return;
//        }
//        ChatGroup chatGroup = chatGroupOptional.get();
//        logger.info("Attempting to delete chat group '{}' (ID: {}) and associated data.", chatGroup.getName(), chatId);
//
//        // --- Step 1: Fetch messages to identify media files ---
//        List<Message> messagesToDelete = messageRepository.findByChatGroupIdOrderByTimestampAsc(chatId); // Fetch messages *before* deleting them
//
//        // --- Step 2: Delete associated media files ---
//        int mediaFilesDeleted = 0;
//        int mediaFilesFailed = 0;
//        for (Message message : messagesToDelete) {
//            if (message.getMediaUrl() != null && !message.getMediaUrl().isBlank()) {
//                try {
//                    String relativePath = message.getMediaUrl();
//                    // Assuming URL format /uploads/filename.ext
//                    if (relativePath.startsWith("/uploads/")) {
//                        String filename = relativePath.substring("/uploads/".length());
//                        boolean deleted = localStorageService.deleteFile(filename);
//                        if (deleted) {
//                            logger.debug("Deleted media file associated with message ID {}: {}", message.getId(), filename);
//                            mediaFilesDeleted++;
//                        } else {
//                            logger.warn("Could not delete media file (not found or error) associated with message ID {}: {}", message.getId(), filename);
//                            mediaFilesFailed++;
//                        }
//                    } else {
//                        logger.warn("Media URL format not recognized for deletion for message ID {}: {}", message.getId(), relativePath);
//                        mediaFilesFailed++;
//                    }
//                } catch (Exception e) {
//                    logger.error("Error deleting media file for message ID {}: {}", message.getId(), e.getMessage());
//                    mediaFilesFailed++;
//                    // Continue deleting other data even if file deletion fails
//                }
//            }
//        }
//        if (!messagesToDelete.isEmpty()) {
//            logger.info("Media file deletion attempt summary for chat group ID {}: {} deleted, {} failed/skipped.", chatId, mediaFilesDeleted, mediaFilesFailed);
//        }
//
//
//        // --- Step 3: Delete database records ---
//        // Delete Reactions associated with the messages in the chat group
//        // If Reactions link directly to messages, fetch message IDs and delete by those.
//        // If Reactions link to chat group (less likely), delete by chat group ID.
//        // Assuming direct link to message:
//        List<String> messageIds = messagesToDelete.stream().map(Message::getId).toList();
//        if (!messageIds.isEmpty()) {
//            long reactionsDeleted = reactionRepository.deleteByMessageIdIn(messageIds);
//            logger.debug("Deleted {} reactions for chat group ID {}", reactionsDeleted, chatId);
//            long readReceiptsDeleted = readReceiptRepository.deleteByMessageIdIn(messageIds);
//            logger.debug("Deleted {} read receipts for chat group ID {}", readReceiptsDeleted, chatId);
//        }
//
//
//        // Delete Messages
//        long messagesDeletedCount = messageRepository.deleteByChatGroupId(chatId);
//        logger.debug("Deleted {} messages for chat group ID {}", messagesDeletedCount, chatId);
//
//        // Delete Participants
//        long participantsDeletedCount = participantRepository.deleteByChatGroupId(chatId);
//        logger.debug("Deleted {} participants for chat group ID {}", participantsDeletedCount, chatId);
//
//        // Delete Chat Group itself
//        chatGroupRepository.deleteById(chatId);
//        logger.info("Successfully deleted chat group '{}' (ID: {}) and associated database records.", chatGroup.getName(), chatId);
//
//    }
}