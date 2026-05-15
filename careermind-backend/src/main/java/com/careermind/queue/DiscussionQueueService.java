package com.careermind.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 讨论任务队列：基于 Redis List 实现 FIFO 削峰。
 *
 * 设计：
 *  - 入队（startDiscussion）：RPUSH 到 queue:list；同时 RPUSH 到 queue:position 用于位置查询
 *  - 出队（Worker 消费）：LPOP；同步从 queue:position 移除
 *  - getPosition(taskId)：扫 queue:position 找索引（轻量，队列容量 < 100）
 *
 * 简化版（非 Streams）：
 *  - 不做消息确认、不做重试持久化
 *  - 重启后队列保留（Redis 持久化），但运行中的任务会丢失
 *  - 适合"削峰"语义，不适合"事务性消息"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionQueueService {

    private final StringRedisTemplate redis;

    @Value("${discussion.queue.key:careermind:discussion:queue}")
    private String queueKey;

    private String positionKey() {
        return queueKey + ":position";
    }

    /** 入队，返回入队后该任务的位置（从 1 开始；1 表示马上消费） */
    public long enqueue(Long taskId) {
        String v = String.valueOf(taskId);
        redis.opsForList().rightPush(queueKey, v);
        Long size = redis.opsForList().rightPush(positionKey(), v);
        long pos = size != null ? size : 0L;
        log.info("[Queue] Task {} 入队，当前位置 {}", taskId, pos);
        return pos;
    }

    /** 出队（阻塞 1s，避免 Worker 空轮询打满 CPU） */
    public Long dequeue() {
        String v = redis.opsForList().leftPop(queueKey);
        if (v == null) {
            return null;
        }
        redis.opsForList().leftPop(positionKey());
        return Long.parseLong(v);
    }

    /** 查 taskId 在队列中的当前位置（1-based）；返回 0 表示不在队列 */
    public long getPosition(Long taskId) {
        List<String> all = redis.opsForList().range(positionKey(), 0, -1);
        if (all == null) return 0;
        String target = String.valueOf(taskId);
        for (int i = 0; i < all.size(); i++) {
            if (Objects.equals(all.get(i), target)) {
                return i + 1;
            }
        }
        return 0;
    }

    /** 队列长度 */
    public long size() {
        Long n = redis.opsForList().size(queueKey);
        return n != null ? n : 0L;
    }

    /** 当前队列里所有 taskId 的快照（按位置排序，1-based 位置 = index+1） */
    public List<Long> snapshotTaskIds() {
        List<String> all = redis.opsForList().range(positionKey(), 0, -1);
        if (all == null) return Collections.emptyList();
        return all.stream().map(Long::parseLong).collect(Collectors.toList());
    }
}
