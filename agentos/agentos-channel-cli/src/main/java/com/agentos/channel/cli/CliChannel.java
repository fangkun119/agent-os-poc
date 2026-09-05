package com.agentos.channel.cli;

/**
 * {@code agentos chat} 命令的实现：CLI Channel（TS 8.4）。
 *
 * <p>读 stdin、写 stdout 实现交互式对话：维护当前 {@link com.agentos.core.session.Session}，
 * 每次用户输入调用一次 {@link com.agentos.core.service.AgentService#process}，
 * 支持 {@code /quit} 命令退出会话。
 *
 * <p>部署口径（TS 10）：核心阶段 CLI Channel 与 {@code AgentScheduler} 同进程
 * <b>直调 {@code AgentService}，不经 Web API</b>（A 版口径）；扩展阶段的 IM Channel
 * （企业微信 / 飞书 / 钉钉 / Slack 等，独立进程部署）才改为调用 {@code agentos-web}
 * 的 Agent 接口——两种接入同走 {@code AgentService.process} 链路，审计与 Session 语义一致。
 *
 * <p>HTTP 接入归 Web Service，不在 Channel 范畴内（TS 8.4）。
 */
public class CliChannel {

    // TODO: 实现阶段补交互循环：stdin 读取、Session 维护、AgentService.process 直调、/quit 退出（TS 8.4）
}
