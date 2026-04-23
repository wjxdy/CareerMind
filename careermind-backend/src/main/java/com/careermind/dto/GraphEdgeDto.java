package com.careermind.dto;

import lombok.Data;

@Data
public class GraphEdgeDto {
    private String id;
    private String from;        // node id
    private String to;          // node id
    private String type;        // SUPPORT/CHALLENGE/REVISE
}
