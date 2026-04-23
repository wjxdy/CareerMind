package com.careermind.dto;

import lombok.Data;

import java.util.List;

@Data
public class ActionPlanDto {
    private List<String> day7;
    private List<String> day30;
    private List<String> day90;
}
