package com.agentos.core.react;

import com.agentos.core.tool.ToolResult;

/**
 * Tool 执行器（TechnicalSolution.md - 4.2 模块组成）：从 ToolRegistry 找到对应 Tool，执行入口按固定次序过四道——
 * ① 入参 JSON Schema 校验（按 getInputSchema() 做基础关键词检查，三路全覆盖、失败不可重试，
 * 设计评审 Q3 决议 2026-09-23，TechnicalSolution.md - 6.7 Sandbox 检查）→ ② sandboxActions(inputJson) 申报动作
 * → ③ Sandbox.check 白名单（仅内置）→ ④ 执行 Tool，
 * 把结果包装成 {@link ToolResult} 返回给 ReAct 循环，并写入 tool_invocations 审计表。
 *
 * <p>失败时返回可重试标识，由 Agent（LLM）自行决定是否重试；框架级自动重试放扩展阶段。
 * 可重试取值统一口径（TechnicalSolution.md - 4.2 模块组成，设计评审 Q4 裁决 2026-09-24）：瞬态失败（超时、暂时不可达）=true、
 * 确定性失败（文件不存在、权限拒绝、server 业务错）=false；retryable 语义融入 error_message 文本供模型判断、
 * 结构化字段仅落审计表、不单独输出进 prompt。
 * 校验失败（schema 或 Sandbox）均复用本类失败审计路径（success=false + retryable=false + error_message；
 * 错误信息含字段名与原因、不含字段实际值），不触发 TechnicalSolution.md - 4.3 关键设计点的终止语义；
 * Sandbox 校验失败抛 SandboxViolationException（TechnicalSolution.md - 6.7 Sandbox 检查）。
 */
public class ToolExecutor {

    // TODO: 实施阶段补 execute(...)（TechnicalSolution.md - 4.2 模块组成）

    public ToolResult execute(String toolName, String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：ToolExecutor.execute（TechnicalSolution.md - 4.2 模块组成）");
    }
}
