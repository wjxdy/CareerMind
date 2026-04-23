package com.careermind.service.impl;

import com.careermind.domain.Discussion;
import com.careermind.domain.Message;
import com.careermind.domain.Round;
import com.careermind.dto.GraphEdgeDto;
import com.careermind.dto.GraphNodeDto;
import com.careermind.dto.GraphResponse;
import com.careermind.dto.GraphRoundStatDto;
import com.careermind.repository.DiscussionRepository;
import com.careermind.repository.MessageRepository;
import com.careermind.repository.RoundRepository;
import com.careermind.service.DiscussionGraphService;
import com.careermind.util.MessageMetaParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiscussionGraphServiceImpl implements DiscussionGraphService {

    private final DiscussionRepository discussionRepository;
    private final RoundRepository roundRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional(readOnly = true)
    public GraphResponse buildGraph(Long taskId) {
        GraphResponse resp = new GraphResponse();
        Discussion d = discussionRepository.findByTaskId(taskId).orElse(null);
        if (d == null) {
            resp.setNodes(Collections.emptyList());
            resp.setEdges(Collections.emptyList());
            resp.setRounds(Collections.emptyList());
            resp.setFinalConvergence(0.0);
            return resp;
        }

        List<Round> rounds = roundRepository.findByDiscussionIdOrderByRoundNumberAsc(d.getId());

        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> edges = new ArrayList<>();
        List<GraphRoundStatDto> roundStats = new ArrayList<>();
        Map<Long, GraphNodeDto> messageIdToNode = new HashMap<>();

        for (Round r : rounds) {
            GraphRoundStatDto rs = new GraphRoundStatDto();
            rs.setRoundNumber(r.getRoundNumber());
            rs.setDivergence(r.getDivergence() == null ? 0.5 : r.getDivergence().doubleValue());
            roundStats.add(rs);

            List<Message> msgs = messageRepository.findByRoundIdOrderByCreatedAtAsc(r.getId());
            for (Message m : msgs) {
                if (m.getAgent() == null) continue; // skip user/interjection
                Long agentId = m.getAgent().getId();
                if (agentId == null || agentId < 0) continue;

                GraphNodeDto n = new GraphNodeDto();
                n.setId("a" + agentId + "-r" + r.getRoundNumber());
                n.setAgentId(agentId);
                n.setAgentType(m.getAgent().getType() != null ? m.getAgent().getType().name() : "CUSTOM");
                n.setAgentName(m.getAgent().getName());
                n.setRoundNumber(r.getRoundNumber());
                n.setMessageId(m.getId());

                String clean = MessageMetaParser.stripConfidence(m.getContent());
                if (clean == null) clean = "";
                n.setSnippet(clean.substring(0, Math.min(100, clean.length())));
                n.setConfidence(m.getConfidence() == null ? 0.6 : m.getConfidence().doubleValue());
                n.setWordCount(clean.length());

                nodes.add(n);
                messageIdToNode.put(m.getId(), n);
            }
        }

        // 边：从 replyToMessageId 推导（同时利用 edgeType 字段）
        for (Round r : rounds) {
            List<Message> msgs = messageRepository.findByRoundIdOrderByCreatedAtAsc(r.getId());
            for (Message m : msgs) {
                if (m.getAgent() == null) continue;
                Long replyTo = m.getReplyToMessageId();
                if (replyTo == null) continue;

                GraphNodeDto from = messageIdToNode.get(m.getId());
                GraphNodeDto to   = messageIdToNode.get(replyTo);
                if (from == null || to == null) continue;

                GraphEdgeDto e = new GraphEdgeDto();
                e.setId("e-" + m.getId());
                e.setFrom(from.getId());
                e.setTo(to.getId());
                e.setType(m.getEdgeType() == null ? "CHALLENGE" : m.getEdgeType());
                edges.add(e);
            }
        }

        resp.setNodes(nodes);
        resp.setEdges(edges);
        resp.setRounds(roundStats);
        double r4Divergence = roundStats.stream()
            .filter(s -> s.getRoundNumber() != null && s.getRoundNumber() == 4)
            .mapToDouble(GraphRoundStatDto::getDivergence)
            .findFirst().orElse(0.5);
        resp.setFinalConvergence(1.0 - r4Divergence);
        return resp;
    }
}
