package com.careermind.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReportResponse {
    private ReportTaskDto task;
    private ReportDiscussionDto discussion;
    private List<ReportRoundDto> rounds;
    private GraphResponse graph;
    private MergeResultDto mergeResult;
    private ReportExtrasDto extras;
}
