package com.agentos.core.context;

import com.agentos.core.profile.Profile;

/**
 * Agent 加载器（TechnicalSolution.md - 8.2 Profile 配置）：扫 .agentos/agents/ 各子目录，把每个 AGENT.md 的 frontmatter
 * （SnakeYAML 解析）派生成一个 {@link Profile} 并注册到 ProfileRegistry。
 * 一个目录 = 一个 Agent（TechnicalSolution.md - 11.1 术语）；Agent 目录不是 Tool。
 */
public class AgentLoader {

    // TODO: 实施阶段补扫描与启动合法性校验，共 10 项（TechnicalSolution.md - 8.2 Profile 配置）：①Provider 存在 ②tools 字段工具名已注册
    //  （只认内置与方式三裸名，MCP 全名报错）③mcp_servers 引用的 server 名存在 ④Channel 支持
    //  ⑤Bootstrap 文件存在 ⑥settings.model 属于对应 Provider 的 MODEL_LIST ⑦schedules.user 格式
    //  ⑧schedules.id 在 Agent 内唯一（同 Agent 内同 id 后条跳过记日志，2026-09-29 E1-A（修订）裁决）
    //  ⑨Agent 目录名与 schedules.id 字符集约束（[a-z0-9-_]、禁含 __、禁以 _ 结尾——组合键
    //   <agent>__<id> 分隔符无歧义前提，规则族同 TechnicalSolution.md - 6.4 Plugin Tool 方式二的 server 名）
    //  ⑩cron 可被 Spring CronTrigger 解析（六段式）、timezone 可被 ZoneId.of 解析
    //   （任一失败仅该条 schedule 跳过 + ERROR 日志，2026-09-30 Q6 裁决）

    public Profile deriveProfile(java.nio.file.Path agentDir) {
        // TODO: AGENT.md frontmatter → Profile 派生（TechnicalSolution.md - 8.2 Profile 配置 / 11.1 术语）
        throw new UnsupportedOperationException("尚未实现：AgentLoader.deriveProfile（TechnicalSolution.md - 8.2 Profile 配置）");
    }
}
