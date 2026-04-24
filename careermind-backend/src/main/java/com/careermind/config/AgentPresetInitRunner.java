package com.careermind.config;

import com.careermind.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时自动调用 initPresetAgents。
 * 该方法内部按 type upsert，缺什么补什么，不重复插入。
 * 让新增的 Agent 类型（如法律团）在重启后即可出现，无需手动调用 /api/agents/init。
 */
@Slf4j
@Component
@Order(1)  // 先于 TaskStatusBackfillRunner（默认 order）
@RequiredArgsConstructor
public class AgentPresetInitRunner implements ApplicationRunner {

    private final AgentService agentService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            agentService.initPresetAgents();
        } catch (Exception e) {
            log.error("启动时初始化预设 Agent 失败: {}", e.getMessage(), e);
        }
    }
}
