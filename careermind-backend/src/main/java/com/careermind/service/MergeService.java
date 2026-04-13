package com.careermind.service;

import com.careermind.domain.MergeResult;
import com.careermind.dto.MergeResultDto;

public interface MergeService {
    MergeResult generateMergeResult(Long taskId);
    MergeResultDto getMergeResult(Long taskId);
    MergeResult selectPlan(Long mergeResultId, Long planId);
}
