package com.careermind.config;

import com.careermind.domain.Discussion;
import com.careermind.domain.Round;
import com.careermind.domain.Task;
import com.careermind.enums.TaskStatus;
import com.careermind.repository.DiscussionRepository;
import com.careermind.repository.RoundRepository;
import com.careermind.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 启动时一次性回填 Task.status，修复历史数据全部停留在 PENDING 的问题。
 * 规则：
 *  - 无讨论 → 保持 PENDING
 *  - 有讨论但无任何已完成轮次 → 保持 PENDING（或将来 DISCUSSING，看 isActive）
 *  - 有讨论且至少 1 轮完成 → DISCUSSING
 *  - 第 4 轮 isCompleted=true → COMPLETED
 *  - 已 ARCHIVED / COMPLETED 不动
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskStatusBackfillRunner implements ApplicationRunner {

    private final TaskRepository taskRepository;
    private final DiscussionRepository discussionRepository;
    private final RoundRepository roundRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Task> tasks = taskRepository.findAll();
        int updated = 0;
        for (Task t : tasks) {
            TaskStatus original = t.getStatus();
            if (original == TaskStatus.ARCHIVED || original == TaskStatus.COMPLETED) continue;

            Discussion d = discussionRepository.findByTaskId(t.getId()).orElse(null);
            if (d == null) continue;

            List<Round> rounds = roundRepository.findByDiscussionIdOrderByRoundNumberAsc(d.getId());
            if (rounds.isEmpty()) continue;

            boolean round4Done = rounds.stream()
                    .anyMatch(r -> r.getRoundNumber() != null && r.getRoundNumber() >= 4
                                && Boolean.TRUE.equals(r.getIsCompleted()));
            boolean anyDone = rounds.stream().anyMatch(r -> Boolean.TRUE.equals(r.getIsCompleted()));

            TaskStatus next = original;
            if (round4Done) next = TaskStatus.COMPLETED;
            else if (anyDone || Boolean.TRUE.equals(d.getIsActive())) next = TaskStatus.DISCUSSING;

            if (next != original) {
                t.setStatus(next);
                taskRepository.save(t);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("[TaskStatusBackfill] 已回填 {} 个 Task 的状态", updated);
        } else {
            log.info("[TaskStatusBackfill] 无需回填");
        }
    }
}
