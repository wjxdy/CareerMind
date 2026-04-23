package com.careermind.service.impl;

import com.careermind.domain.*;
import com.careermind.dto.*;
import com.careermind.repository.*;
import com.careermind.service.DiscussionGraphService;
import com.careermind.service.MergeService;
import com.careermind.service.ReportService;
import com.careermind.util.MessageMetaParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TaskRepository taskRepository;
    private final DiscussionRepository discussionRepository;
    private final RoundRepository roundRepository;
    private final MessageRepository messageRepository;
    private final MergeService mergeService;
    private final DiscussionGraphService graphService;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    private static final Map<String, String> ROUND_LABELS = Map.of(
        "INDEPENDENT", "独立诊断",
        "CHALLENGE",   "质疑挑战",
        "REVISION",    "修正完善",
        "FINAL",       "最终陈述"
    );

    @Override
    @Transactional(readOnly = true)
    public ReportResponse build(Long taskId, boolean refreshExtras) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("任务不存在"));
        Discussion d = discussionRepository.findByTaskId(taskId).orElse(null);

        ReportResponse resp = new ReportResponse();

        ReportTaskDto t = new ReportTaskDto();
        t.setId(task.getId());
        t.setTitle(task.getTitle());
        t.setBackground(task.getBackground());
        t.setGoal(task.getGoal());
        t.setConstraints(task.getConstraints());
        t.setCreatedAt(task.getCreatedAt());
        if (task.getUser() != null) t.setUsername(task.getUser().getUsername());
        resp.setTask(t);

        ReportDiscussionDto ddto = new ReportDiscussionDto();
        if (d != null) {
            ddto.setCurrentRound(d.getCurrentRound());
            ddto.setStartedAt(d.getCreatedAt());
        }
        resp.setDiscussion(ddto);

        List<ReportRoundDto> rounds = new ArrayList<>();
        int totalMessages = 0;
        if (d != null) {
            for (Round r : roundRepository.findByDiscussionIdOrderByRoundNumberAsc(d.getId())) {
                ReportRoundDto rd = new ReportRoundDto();
                rd.setRoundNumber(r.getRoundNumber());
                String typeName = r.getRoundType() != null ? r.getRoundType().name() : "";
                rd.setLabel(ROUND_LABELS.getOrDefault(typeName, typeName));
                rd.setDivergence(r.getDivergence() == null ? 0.5 : r.getDivergence().doubleValue());

                List<ReportRoundDto.Item> items = new ArrayList<>();
                for (Message m : messageRepository.findByRoundIdOrderByCreatedAtAsc(r.getId())) {
                    if (m.getAgent() == null) continue;
                    ReportRoundDto.Item it = new ReportRoundDto.Item();
                    it.setAgentId(m.getAgent().getId());
                    it.setAgentName(m.getAgent().getName());
                    it.setAgentType(m.getAgent().getType() != null ? m.getAgent().getType().name() : "CUSTOM");
                    it.setContent(MessageMetaParser.stripConfidence(m.getContent()));
                    it.setConfidence(m.getConfidence() == null ? 0.6 : m.getConfidence().doubleValue());
                    items.add(it);
                    totalMessages++;
                }
                rd.setMessages(items);
                rounds.add(rd);
            }
        }
        ddto.setTotalMessages(totalMessages);
        resp.setRounds(rounds);

        resp.setGraph(graphService.buildGraph(taskId));

        try {
            resp.setMergeResult(mergeService.getMergeResult(taskId));
        } catch (Exception ignore) {
            resp.setMergeResult(null);
        }

        // extras 缓存：24h
        ReportExtrasDto extras = null;
        String key = "report:extras:" + taskId;
        if (!refreshExtras) {
            try {
                String cached = redis.opsForValue().get(key);
                if (cached != null) {
                    extras = mapper.readValue(cached, ReportExtrasDto.class);
                }
            } catch (Exception ignore) {}
        }
        if (extras == null) {
            extras = new ReportExtrasDto();
            extras.setExecutiveSummary(mergeService.generateExecutiveSummary(taskId));
            extras.setActionPlan(mergeService.generateActionPlan(taskId));
            try {
                redis.opsForValue().set(key, mapper.writeValueAsString(extras), Duration.ofHours(24));
            } catch (Exception ignore) {}
        }
        resp.setExtras(extras);

        return resp;
    }
}
