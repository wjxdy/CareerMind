package com.careermind.queue;

import com.careermind.service.DiscussionEngine;
import com.careermind.websocket.DiscussionWebSocketHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 讨论队列消费者：
 *  - 定时 poll Redis 队列
 *  - 在独立 workerPool（与 llmExecutor 解耦，避免线程池嵌套死锁）上同时跑 maxConcurrent 个咨询
 *  - 每轮 poll 后向队列中剩余 task 推送当前位置
 *
 * 线程池布局：
 *   workerPool（默认 2 线程，跑「一整轮咨询」）
 *      └─ 内部 Round 1/4 时占用 llmExecutor（4-8 线程，跑「单 Agent LLM 调用」）
 *           └─ 受 kimiSemaphore(3) 限制对上游真正的并发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscussionWorker {

    private final DiscussionQueueService queue;
    private final DiscussionEngine engine;
    private final DiscussionWebSocketHandler ws;

    @Value("${discussion.queue.worker-enabled:true}")
    private boolean enabled;

    @Value("${discussion.queue.max-concurrent:2}")
    private int maxConcurrent;

    private ThreadPoolExecutor workerPool;

    @PostConstruct
    void init() {
        AtomicInteger seq = new AtomicInteger();
        workerPool = new ThreadPoolExecutor(
                maxConcurrent, maxConcurrent,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "discussion-worker-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
        );
        log.info("DiscussionWorker 启动，maxConcurrent={}", maxConcurrent);
    }

    @Scheduled(fixedDelayString = "${discussion.queue.poll-interval-ms:500}")
    public void poll() {
        if (!enabled) return;

        // 只在 workerPool 有空闲时取任务，否则让任务继续留在 Redis 队列里
        while (workerPool.getActiveCount() < maxConcurrent && queue.size() > 0) {
            Long taskId = queue.dequeue();
            if (taskId == null) break;
            log.info("[Worker] dequeue task={}, 队列剩余={}", taskId, queue.size());

            workerPool.execute(() -> {
                try {
                    // 通知该 task 自己已经开始（position=0）
                    ws.sendQueueStatus(taskId, 0, queue.size());
                    engine.executeCurrentRound(taskId);
                } catch (Throwable e) {
                    log.error("[Worker] task {} 执行失败", taskId, e);
                }
            });
        }

        broadcastQueuePositions();
    }

    /** 把队列里每个 task 的当前位置广播给对应的前端 */
    private void broadcastQueuePositions() {
        List<Long> snapshot = queue.snapshotTaskIds();
        if (snapshot.isEmpty()) return;
        int total = snapshot.size();
        for (int i = 0; i < total; i++) {
            ws.sendQueueStatus(snapshot.get(i), i + 1, total);
        }
    }

    @PreDestroy
    void shutdown() {
        if (workerPool != null) {
            workerPool.shutdown();
            try {
                if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    workerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                workerPool.shutdownNow();
            }
        }
    }
}
