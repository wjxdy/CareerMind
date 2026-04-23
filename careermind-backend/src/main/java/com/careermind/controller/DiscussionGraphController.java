package com.careermind.controller;

import com.careermind.dto.GraphResponse;
import com.careermind.service.DiscussionGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discussions/tasks/{taskId}")
@RequiredArgsConstructor
public class DiscussionGraphController {

    private final DiscussionGraphService graphService;

    @GetMapping("/graph")
    public GraphResponse getGraph(@PathVariable Long taskId) {
        return graphService.buildGraph(taskId);
    }
}
