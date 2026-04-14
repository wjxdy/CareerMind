package com.careermind.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageDto {
    private Long id;
    private Long agentId;
    private String agentName;
    private String agentAvatar;
    private String agentType;
    private String content;
    private Long replyToMessageId;
    private String messageType;
    private Boolean isFinal;
    private LocalDateTime createdAt;
}
