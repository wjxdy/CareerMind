package com.careermind.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportDiscussionDto {
    private Integer currentRound;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer totalMessages;
}
