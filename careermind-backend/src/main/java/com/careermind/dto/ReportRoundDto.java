package com.careermind.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReportRoundDto {
    private Integer roundNumber;
    private String label;
    private Double divergence;
    private List<Item> messages;

    @Data
    public static class Item {
        private Long agentId;
        private String agentName;
        private String agentType;
        private String content;
        private Double confidence;
    }
}
