package com.careermind.dto;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class PlanDto {
    private Long id;
    private String title;
    private String description;
    private Integer confidence;
    private List<String> supporters;
    private List<String> opponents;
    private List<String> milestones;
    private List<String> risks;
    private String applicableConditions;
    private Boolean isSelected;
}
