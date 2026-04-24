-- V1 把 agents.type 定义为 ENUM，每加一种新类型都要 ALTER。
-- 改成 VARCHAR(50)，配合 JPA @Enumerated(EnumType.STRING) 一劳永逸。
-- 注意：项目当前 ddl-auto=update 不会自动改 ENUM；执行此脚本一次即可。
ALTER TABLE agents MODIFY COLUMN type VARCHAR(50) NOT NULL COMMENT '角色类型';
