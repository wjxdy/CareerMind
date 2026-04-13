package com.careermind.dto;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class DiscussionDto {
    private Long id;
    private Long taskId;
    private Integer currentRound;
    private Boolean isActive;
    private Boolean isPaused;
    private List<RoundDto> rounds;
}
