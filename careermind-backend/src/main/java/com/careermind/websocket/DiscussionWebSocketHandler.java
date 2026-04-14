package com.careermind.websocket;

import com.careermind.dto.MessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DiscussionWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储taskId到session的映射
    private final Map<Long, WebSocketSession> taskSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionTaskMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: {}", session.getId());

        // 从URL参数获取taskId
        String query = session.getUri().getQuery();
        if (query != null && query.contains("taskId=")) {
            String taskIdStr = query.split("taskId=")[1].split("&")[0];
            Long taskId = Long.parseLong(taskIdStr);
            taskSessions.put(taskId, session);
            sessionTaskMap.put(session.getId(), taskId);
            log.info("Session {} 订阅了 Task {}", session.getId(), taskId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到消息: {}", payload);
        // 处理客户端消息
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket连接关闭: {}, 状态: {}", session.getId(), status);
        Long taskId = sessionTaskMap.remove(session.getId());
        if (taskId != null) {
            taskSessions.remove(taskId);
        }
    }

    public void sendMessageToTask(Long taskId, MessageDto messageDto) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode wrapper = objectMapper.createObjectNode();
                wrapper.put("type", "message");
                wrapper.set("data", objectMapper.valueToTree(messageDto));
                String message = objectMapper.writeValueAsString(wrapper);
                session.sendMessage(new TextMessage(message));
                log.debug("消息已发送到Task {}: {}", taskId, message);
            } catch (IOException e) {
                log.error("发送WebSocket消息失败", e);
            }
        }
    }

    public void sendToTask(Long taskId, Object data) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(data);
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送WebSocket消息失败", e);
            }
        }
    }

    /**
     * 发送流式输出开始事件
     */
    public void sendStreamStart(Long taskId, Long agentId, String agentName, String agentType, String agentAvatar) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "stream_start");
                ObjectNode data = message.putObject("data");
                data.put("agentId", agentId);
                data.put("agentName", agentName);
                data.put("agentType", agentType);
                data.put("agentAvatar", agentAvatar);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                log.debug("流式开始事件已发送到Task {}", taskId);
            } catch (IOException e) {
                log.error("发送流式开始事件失败", e);
            }
        }
    }

    /**
     * 发送流式内容片段
     */
    public void sendStreamChunk(Long taskId, String content) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "stream_chunk");
                message.put("content", content);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送流式片段失败", e);
            }
        }
    }

    /**
     * 发送流式输出结束事件
     */
    public void sendStreamEnd(Long taskId, Long messageId) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "stream_end");
                message.put("messageId", messageId);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                log.debug("流式结束事件已发送到Task {}", taskId);
            } catch (IOException e) {
                log.error("发送流式结束事件失败", e);
            }
        }
    }

    public void sendInterjectionStreamStart(Long taskId, Long agentId, String agentName, String agentType, String agentAvatar) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "interjection_stream_start");
                ObjectNode data = message.putObject("data");
                data.put("agentId", agentId);
                data.put("agentName", agentName);
                data.put("agentType", agentType);
                data.put("agentAvatar", agentAvatar);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送插话流式开始事件失败", e);
            }
        }
    }

    public void sendInterjectionStreamChunk(Long taskId, String content) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "interjection_stream_chunk");
                message.put("content", content);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送插话流式片段失败", e);
            }
        }
    }

    public void sendInterjectionStreamEnd(Long taskId, Long messageId) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "interjection_stream_end");
                message.put("messageId", messageId);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送插话流式结束事件失败", e);
            }
        }
    }

    public void sendDiscussionResumed(Long taskId) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "discussion_resumed");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送讨论恢复事件失败", e);
            }
        }
    }

    /**
     * 发送结果流式开始事件
     */
    public void sendResultStreamStart(Long taskId) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "result_stream_start");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                log.debug("结果流式开始事件已发送到Task {}", taskId);
            } catch (IOException e) {
                log.error("发送结果流式开始事件失败", e);
            }
        }
    }

    /**
     * 发送结果流式内容片段
     */
    public void sendResultStreamChunk(Long taskId, String content) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "result_stream_chunk");
                message.put("content", content);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送结果流式片段失败", e);
            }
        }
    }

    /**
     * 发送结果流式输出结束事件
     */
    public void sendResultStreamEnd(Long taskId, Long resultId) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "result_stream_end");
                message.put("resultId", resultId);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                log.debug("结果流式结束事件已发送到Task {}", taskId);
            } catch (IOException e) {
                log.error("发送结果流式结束事件失败", e);
            }
        }
    }
}
