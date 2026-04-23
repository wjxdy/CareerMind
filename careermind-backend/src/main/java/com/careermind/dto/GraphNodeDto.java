package com.careermind.dto;

import lombok.Data;

@Data
public class GraphNodeDto {
    private String id;          // "a1-r1"
    private Long agentId;
    private String agentType;
    private String agentName;
    private Integer roundNumber;
    private Long messageId;
    private String snippet;     // 前 100 字
    private Double confidence;
    private Integer wordCount;
}
