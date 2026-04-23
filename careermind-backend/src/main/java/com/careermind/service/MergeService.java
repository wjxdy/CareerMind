package com.careermind.service;

import com.careermind.domain.MergeResult;
import com.careermind.dto.MergeResultDto;

public interface MergeService {
    MergeResult generateMergeResult(Long taskId);
    MergeResultDto getMergeResult(Long taskId);
    MergeResult selectPlan(Long mergeResultId, Long planId);

    /** P3: 生成 200 字执行摘要 */
    String generateExecutiveSummary(Long taskId);

    /** P3: 生成 7/30/90 天行动清单 */
    com.careermind.dto.ActionPlanDto generateActionPlan(Long taskId);
}
