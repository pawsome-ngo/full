package com.pawsome.rescue.features.chat.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ParticipantDto {
    private ChatGroupDto chatGroup;
    private boolean hasUnreadMessages;
    private String lastMessage;
    private LocalDateTime lastMessageTimestamp;
}