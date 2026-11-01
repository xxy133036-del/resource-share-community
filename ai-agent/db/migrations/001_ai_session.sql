-- ============================================================
-- AI 客服会话存储（参考 Codex 会话模型）
--
-- 设计要点：
-- 1) ai_session           会话索引与派生状态（对应 Codex 的 session_index + state db）
-- 2) ai_session_event     追加写入的事件日志，唯一事实来源（对应 Codex 的 rollout jsonl）
-- 3) 摘要压缩             summary / summary_upto_ordinal 记录压缩点，原文永不删除
--
-- 与业务表（file_info / user_info / message_info）完全解耦，互不影响。
-- ============================================================

CREATE TABLE IF NOT EXISTS `ai_session` (
  `id`                   bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `session_id`           varchar(64)  NOT NULL COMMENT '会话业务ID（conv_xxx）',
  `user_id`              varchar(64)  NOT NULL COMMENT '归属用户ID（来自服务端签名，不可被请求体覆盖）',
  `title`                varchar(255) DEFAULT NULL COMMENT '会话标题（取首条用户消息）',
  `status`               varchar(16)  NOT NULL DEFAULT 'active' COMMENT 'active-进行中 archived-已归档',
  `event_seq`            int(11)      NOT NULL DEFAULT 0 COMMENT '事件序号发号器，仅在事务内自增',
  `turn_count`           int(11)      NOT NULL DEFAULT 0 COMMENT '对话轮次',
  `message_count`        int(11)      NOT NULL DEFAULT 0 COMMENT '消息条数（user+assistant）',
  `summary`              mediumtext   DEFAULT NULL COMMENT '压缩摘要，替代较早的原始事件进入模型上下文',
  `summary_upto_ordinal` int(11)      NOT NULL DEFAULT 0 COMMENT '摘要覆盖到的事件序号（小于此序号的事件已被摘要）',
  `total_tokens`         int(11)      NOT NULL DEFAULT 0 COMMENT '累计 token 消耗',
  `created_at`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_session_sid` (`session_id`),
  KEY `idx_ai_session_user` (`user_id`, `updated_at` DESC),
  KEY `idx_ai_session_status` (`status`, `updated_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI客服会话索引';

CREATE TABLE IF NOT EXISTS `ai_session_event` (
  `id`         bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '自增主键（全局写入顺序）',
  `session_id` varchar(64) NOT NULL COMMENT '所属会话',
  `ordinal`    int(11)     NOT NULL COMMENT '会话内单调递增序号，由 ai_session.event_seq 原子分配',
  `turn_id`    varchar(64) DEFAULT NULL COMMENT '所属轮次ID，同一次问答的事件共享',
  `type`       varchar(32) NOT NULL COMMENT 'user_message/assistant_message/tool_call/tool_result/status/usage/compacted/error',
  `role`       varchar(16) DEFAULT NULL COMMENT 'user/assistant/tool/system',
  `content`    mediumtext  DEFAULT NULL COMMENT '正文（消息内容或摘要）',
  `payload`    json        DEFAULT NULL COMMENT '结构化附加信息（工具名/参数/用量等）',
  `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '写入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_event_ordinal` (`session_id`, `ordinal`),
  KEY `idx_ai_event_session_type` (`session_id`, `type`, `ordinal`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI客服会话事件日志（追加写，不更新不删除）';