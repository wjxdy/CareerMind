package com.careermind.dto;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class MergeResultDto {
    private Long id;
    private String summary;
    private List<PlanDto> plans;
    private List<String> blindSpots;
    private Double convergenceRate;
}
