package com.careermind.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportTaskDto {
    private Long id;
    private String title;
    private String background;
    private String goal;
    private String constraints;
    private LocalDateTime createdAt;
    private String username;
}
