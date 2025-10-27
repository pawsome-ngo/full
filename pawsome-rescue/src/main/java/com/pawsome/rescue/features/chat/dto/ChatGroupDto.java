package com.pawsome.rescue.features.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatGroupDto {
    private String id;
    private String name;
    private Long purposeId; // Add this line
}