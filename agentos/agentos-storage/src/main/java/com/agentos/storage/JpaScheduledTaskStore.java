package com.agentos.storage;

import com.agentos.core.schedule.ScheduledTaskStore;

import java.util.List;

/**
 * {@link ScheduledTaskStore} 的 JPA 实现：契约在 core、实现在 storage（依赖倒置，TS 8.5），
 * 落 scheduled_tasks / task_executions 两张表（TS 9.2，收尾阶段补齐）。
 *
 * <p>定义来源是 AGENT.md frontmatter 的 schedules——本类只存"状态 + 历史"，
 * 不作为定义源，重启时从文件重新注册。骨架阶段六方法均未实现。
 */
public class JpaScheduledTaskStore implements ScheduledTaskStore {

    @Override
    public void register(String taskId, String profileName, String cron, String zone, String message) {
        throw new UnsupportedOperationException("尚未实现：JpaScheduledTaskStore.register（TS 8.5）");
    }

    @Override
    public void recordExecution(String taskId, String sessionId, boolean success, String errorMessage, long durationMs) {
        throw new UnsupportedOperationException("尚未实现：JpaScheduledTaskStore.recordExecution（TS 8.5）");
    }

    @Override
    public boolean isEnabled(String taskId) {
        throw new UnsupportedOperationException("尚未实现：JpaScheduledTaskStore.isEnabled（TS 8.5）");
    }

    @Override
    public void setEnabled(String taskId, boolean enabled) {
        throw new UnsupportedOperationException("尚未实现：JpaScheduledTaskStore.setEnabled（TS 8.5）");
    }

    @Override
    public List<String> list() {
        throw new UnsupportedOperationException("尚未实现：JpaScheduledTaskStore.list（TS 8.5）");
    }

    @Override
    public List<String> executions(String taskId) {
        throw new UnsupportedOperationException("尚未实现：JpaScheduledTaskStore.executions（TS 8.5）");
    }
}
