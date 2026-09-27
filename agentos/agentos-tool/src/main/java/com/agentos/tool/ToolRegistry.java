package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;

/**
 * Tool 统一注册表（TechnicalSolution.md - 6.6 ToolRegistry）。
 *
 * <p>统一注册所有 AgentOSTool：内置 Tool + {@code @Tool} 注解扫描（Plugin 方式三）+ MCP 包装
 * （Plugin 方式二，暴露名 {@code <server名>__<工具名>}），ReAct 循环不感知 Tool 的来源。
 * 工具子集每次组装 prompt 时现算（TechnicalSolution.md - 6.6 ToolRegistry 的工具子集规则，Q2 裁决 2026-09-23）：{@code tools} 字段
 * 只认内置与方式三裸名（缺省=全部内置与方式三可见，显式列举即收窄）；{@code mcp_servers} 声明的
 * server 其全部工具整组并入。Tool 治理的雏形，完整 allow/deny 策略放扩展阶段。
 */
public class ToolRegistry {

    /**
     * 注册一个 Tool。
     *
     * @param tool 被包装成 AgentOSTool 的工具实例
     */
    public void register(AgentOSTool tool) {
        throw new UnsupportedOperationException("尚未实现：ToolRegistry.register（TechnicalSolution.md - 6.6 ToolRegistry）");
    }

    /**
     * 按名称查找 Tool。
     *
     * @param name Tool 唯一名称（如 read_file / notify）
     * @return 对应的 AgentOSTool 实例
     */
    public AgentOSTool find(String name) {
        throw new UnsupportedOperationException("尚未实现：ToolRegistry.find（TechnicalSolution.md - 6.6 ToolRegistry）");
    }
}
