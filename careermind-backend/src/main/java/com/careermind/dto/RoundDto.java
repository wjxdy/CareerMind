package com.careermind.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoundDto {
    private Long id;
    private Integer roundNumber;
    private String roundType;
    private Boolean isCompleted;
    private List<MessageDto> messages;
    private LocalDateTime createdAt;
}
