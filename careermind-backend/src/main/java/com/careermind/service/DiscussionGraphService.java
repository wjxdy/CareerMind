package com.careermind.service;

import com.careermind.dto.GraphResponse;

public interface DiscussionGraphService {
    GraphResponse buildGraph(Long taskId);
}
