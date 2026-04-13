-- CareerMind 数据库初始化脚本
-- 创建数据库: CREATE DATABASE careermind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Agent表
CREATE TABLE IF NOT EXISTS agents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '角色名称',
    type VARCHAR(50) NOT NULL COMMENT '角色类型',
    system_prompt TEXT COMMENT '系统提示词',
    model_type VARCHAR(50) COMMENT '使用的模型类型',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    is_preset BOOLEAN DEFAULT FALSE COMMENT '是否为预设Agent',
    user_id BIGINT COMMENT '创建者ID，预设Agent为NULL',
    description VARCHAR(500) COMMENT '角色描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_is_preset (is_preset),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent角色表';

-- Task表
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    background TEXT COMMENT '背景信息',
    goal TEXT COMMENT '目标/困惑',
    constraints TEXT COMMENT '约束条件',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- Task-Agent关联表
CREATE TABLE IF NOT EXISTS task_agents (
    task_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    weight INT DEFAULT 100 COMMENT '权重',
    PRIMARY KEY (task_id, agent_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务Agent关联表';

-- Discussion表
CREATE TABLE IF NOT EXISTS discussions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE COMMENT '任务ID',
    current_round INT DEFAULT 0 COMMENT '当前轮次',
    is_active BOOLEAN DEFAULT FALSE COMMENT '是否活跃',
    is_paused BOOLEAN DEFAULT FALSE COMMENT '是否暂停',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='讨论表';

-- Round表
CREATE TABLE IF NOT EXISTS rounds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    discussion_id BIGINT NOT NULL COMMENT '讨论ID',
    round_number INT NOT NULL COMMENT '轮次编号',
    round_type VARCHAR(20) NOT NULL COMMENT '轮次类型',
    is_completed BOOLEAN DEFAULT FALSE COMMENT '是否完成',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_discussion_round (discussion_id, round_number),
    FOREIGN KEY (discussion_id) REFERENCES discussions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮次表';

-- Message表
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    round_id BIGINT NOT NULL COMMENT '轮次ID',
    agent_id BIGINT NOT NULL COMMENT 'AgentID',
    content TEXT NOT NULL COMMENT '发言内容',
    reply_to_message_id BIGINT COMMENT '回复的消息ID',
    is_final BOOLEAN DEFAULT FALSE COMMENT '是否为最终观点',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_round_id (round_id),
    INDEX idx_agent_id (agent_id),
    FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- MergeResult表
CREATE TABLE IF NOT EXISTS merge_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE COMMENT '任务ID',
    summary TEXT COMMENT '整体总结',
    blind_spots TEXT COMMENT '认知盲区JSON',
    convergence_rate DECIMAL(3,2) COMMENT '观点收敛度',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='整合结果表';

-- Plan表
CREATE TABLE IF NOT EXISTS plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merge_result_id BIGINT NOT NULL COMMENT '整合结果ID',
    title VARCHAR(100) NOT NULL COMMENT '方案标题',
    description TEXT COMMENT '方案描述',
    confidence INT COMMENT '置信度',
    supporters TEXT COMMENT '支持者JSON',
    opponents TEXT COMMENT '反对者JSON',
    milestones TEXT COMMENT '里程碑JSON',
    risks TEXT COMMENT '风险JSON',
    applicable_conditions TEXT COMMENT '适用条件',
    is_selected BOOLEAN DEFAULT FALSE COMMENT '是否被选中',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_merge_result (merge_result_id),
    FOREIGN KEY (merge_result_id) REFERENCES merge_results(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='方案表';
