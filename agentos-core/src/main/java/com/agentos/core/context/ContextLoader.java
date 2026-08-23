package com.agentos.core.context;

/**
 * 上下文加载（TS 8.3）：按 Profile 的 bootstrap 字段读取 Bootstrap（AGENTS.md / SOUL.md / USER.md），
 * 同时现读 Agent 自己的 AGENT.md 正文；每次组装 prompt 时重新扫描 Agent skills/ 的相对软连接，
 * 只注入 Skill 的 name/description 与本地读取路径。全部无缓存，修改后下一轮立即生效。
 *
 * <p>AGENT.md 是 prompt 的输入源、不是可执行 Tool；其解析归本类与 {@link AgentLoader}（AiProgrammingGuide 4.6 纠偏表）。
 */
public class ContextLoader {

    // TODO: 实施阶段补 loadBootstrap / loadAgentMarkdown / scanBoundSkills 等方法（TS 8.3）
}
