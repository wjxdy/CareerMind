package com.careermind.controller;

import com.careermind.dto.ApiResponse;
import com.careermind.dto.MergeResultDto;
import com.careermind.service.MergeService;
import com.careermind.service.impl.MergeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/merge")
@RequiredArgsConstructor
public class MergeController {

    private final MergeService mergeService;
    private final MergeServiceImpl mergeServiceImpl;

    @PostMapping("/tasks/{taskId}/generate")
    public ResponseEntity<ApiResponse<Object>> generateMergeResult(@PathVariable Long taskId) {
        // 使用流式生成
        mergeServiceImpl.generateMergeResultStream(taskId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Object>> getMergeResult(@PathVariable Long taskId) {
        try {
            MergeResultDto result = mergeService.getMergeResult(taskId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            // 结果不存在时返回404
            return ResponseEntity.status(404).body(ApiResponse.error(404, "结果尚未生成"));
        }
    }

    @PostMapping("/{mergeResultId}/select-plan")
    public ApiResponse<Object> selectPlan(
            @PathVariable Long mergeResultId,
            @RequestBody Map<String, Long> body) {
        Long planId = body.get("planId");
        return ApiResponse.success(mergeService.selectPlan(mergeResultId, planId));
    }
}
