package com.careermind.dto;

import com.careermind.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskDto {
    private Long id;
    private String title;
    private String background;
    private String goal;
    private String constraints;
    private TaskStatus status;
    private List<AgentDto> agents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class AgentDto {
        private Long id;
        private String name;
        private String type;
        private String description;
    }
}
