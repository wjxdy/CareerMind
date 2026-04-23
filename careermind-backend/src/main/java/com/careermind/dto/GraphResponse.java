package com.careermind.dto;

import lombok.Data;

import java.util.List;

@Data
public class GraphResponse {
    private List<GraphNodeDto> nodes;
    private List<GraphEdgeDto> edges;
    private List<GraphRoundStatDto> rounds;
    private Double finalConvergence;  // 1 - divergence(round4)
}
