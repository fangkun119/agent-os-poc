package com.agentos.core.react;

/**
 * Prompt 组装器（TS 4.2），按<b>五部分</b>顺序拼接：
 * <ol>
 *   <li>system prompt（AGENT.md 正文 + 已绑定 Skill 的 name/description/本地路径；末尾附当前日期时间——定时场景的"今天"全靠它）</li>
 *   <li>Bootstrap 文件（AGENTS.md / SOUL.md / USER.md）</li>
 *   <li>Memory 注入（<b>仅长期记忆</b>；会话历史不在此段，由第 4 段独立注入一次）</li>
 *   <li>对话历史（按 maxHistoryTurns 截断后的 Session messages）</li>
 *   <li>当前 Profile 可用的 Tool 列表（Function Calling 格式）</li>
 * </ol>
 */
public class PromptBuilder {

    // TODO: 实施阶段补 build(...)（TS 4.2 五部分拼接）
}
