package com.pawsome.rescue.features.globalchat.dto;

import com.pawsome.rescue.auth.dto.UserDto;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatMessage.MediaType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GlobalChatMessageDto {
    private String id;
    private String text;
    private LocalDateTime timestamp;
    private UserDto sender;
    private String clientMessageId;
    private String parentMessageId;
    private Map<String, List<UserDto>> reactions;
    private List<UserDto> seenBy;
    private String mediaUrl;
    private MediaType mediaType;
}