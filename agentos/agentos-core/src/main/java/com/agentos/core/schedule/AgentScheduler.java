package com.agentos.core.schedule;

/**
 * 定时任务——第三种触发源"钟推"（TS 8.5）。
 *
 * <p>基于 Spring 的 ThreadPoolTaskScheduler + CronTrigger 动态注册（不用静态 @Scheduled，
 * 触发规则按 Profile 配置动态生成）。Profile 的 schedules 字段声明 id、cron 表达式、时区、消息内容。
 * 并发控制：按任务 id 维度的进程内 ReentrantLock 防重叠执行（单实例口径，非分布式锁）。
 * 失败只记日志不崩调度器；执行历史经 {@link ScheduledTaskStore} 落 SQLite。
 */
public class AgentScheduler {

    // TODO: 实施阶段补 registerProfile / unregisterProfile / runNow 等方法（TS 8.5）
}
