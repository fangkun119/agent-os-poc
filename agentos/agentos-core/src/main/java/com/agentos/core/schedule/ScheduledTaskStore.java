package com.agentos.core.schedule;

import java.util.List;

/**
 * 定时任务状态契约（TS 8.5）：契约在 core、实现在 storage（依赖倒置）。
 * JPA 实现 JpaScheduledTaskStore 位于 agentos-storage 模块。
 *
 * <p>定义来源仍是 AGENT.md frontmatter 的 schedules——本接口只存"状态 + 历史"，不作为定义源。
 * 方法签名为骨架最小集，实施阶段可随实体结构细化（返回值暂以字符串列表占位）。
 */
public interface ScheduledTaskStore {

    /** 启动扫描时登记任务（task_id 即 frontmatter schedules 里声明的 id）。 */
    void register(String taskId, String profileName, String cron, String zone, String message);

    /** 每次执行（成功失败都记）留痕：审计原则 day one 落库。 */
    void recordExecution(String taskId, String sessionId, boolean success, String errorMessage, long durationMs);

    boolean isEnabled(String taskId);

    void setEnabled(String taskId, boolean enabled);

    /** 列任务与运行状态（结构待实施阶段细化）。 */
    List<String> list();

    /** 查执行历史（结构待实施阶段细化）。 */
    List<String> executions(String taskId);
}
