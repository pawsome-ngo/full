package com.pawsome.rescue.features.chat.dto;

import com.pawsome.rescue.auth.dto.UserDto;
// --- ADD IMPORT ---
import com.pawsome.rescue.features.chat.entity.Message.MediaType;
// --- END IMPORT ---
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MessageDto {
    private String id;
    private String text;
    private LocalDateTime timestamp;
    private UserDto sender;
    private String clientMessageId;
    private String parentMessageId;
    private Map<String, List<UserDto>> reactions;
    private List<UserDto> seenBy;

    // --- NEW FIELDS ---
    private String mediaUrl;
    private MediaType mediaType;
    // --- END NEW FIELDS ---
}