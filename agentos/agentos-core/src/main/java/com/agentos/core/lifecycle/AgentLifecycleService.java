package com.agentos.core.lifecycle;

/**
 * Agent 生命周期编排（TechnicalSolution.md - 10 项目工程结构 / 11.3 扩展阶段，<b>扩展阶段交付</b>）：
 * 定义一个 Agent = Agent 目录落盘 + deriveProfile + 注册 + Scheduler 注册。
 *
 * <p>核心阶段先立好 ProfileRegistry（register/remove/exists）与 AgentScheduler
 * （registerProfile/unregisterProfile）的运行时注册钩子，供本服务与扩展阶段的
 * WorkspaceWatcher / API 上传两条录入路径统一调用（同一段注册代码，行为一模一样）。
 */
public class AgentLifecycleService {

    // TODO: 扩展阶段实现（TechnicalSolution.md - 11.3 扩展阶段）：generate / create / update / delete / register(agentDir)
}
