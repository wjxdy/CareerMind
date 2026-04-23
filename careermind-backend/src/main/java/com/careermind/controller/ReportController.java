package com.careermind.controller;

import com.careermind.dto.ReportResponse;
import com.careermind.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{taskId}")
    public ReportResponse get(@PathVariable Long taskId,
                              @RequestParam(defaultValue = "false") boolean refresh) {
        return reportService.build(taskId, refresh);
    }

    @PostMapping("/{taskId}/regenerate-summary")
    public ReportResponse regenerate(@PathVariable Long taskId) {
        return reportService.build(taskId, true);
    }
}
