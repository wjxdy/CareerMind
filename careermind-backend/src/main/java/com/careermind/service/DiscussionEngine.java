package com.careermind.service;

import com.careermind.domain.Discussion;
import com.careermind.dto.DiscussionDto;

public interface DiscussionEngine {
    Discussion createDiscussion(Long taskId);
    DiscussionDto startDiscussion(Long taskId);
    DiscussionDto getDiscussion(Long taskId);
    DiscussionDto pauseDiscussion(Long taskId);
    DiscussionDto resumeDiscussion(Long taskId);
    DiscussionDto stopDiscussion(Long taskId);
    DiscussionDto nextRound(Long taskId);
    DiscussionDto addUserMessage(Long taskId, String content);

    /** Worker 入口：同步执行当前 Discussion 的"当前轮"（不创建/不切轮） */
    void executeCurrentRound(Long taskId);
}
