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
}
