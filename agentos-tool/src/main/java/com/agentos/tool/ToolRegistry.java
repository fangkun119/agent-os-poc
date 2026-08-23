package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;

/**
 * Tool 统一注册表（TS 6.6）。
 *
 * <p>统一注册所有 AgentOSTool：内置 Tool + {@code @Tool} 注解扫描（Plugin 方式三）+ MCP 包装
 * （Plugin 方式二），ReAct 循环不感知 Tool 的来源。Profile 启动 Agent 时按 {@code tools} 字段
 * 从 Registry 过滤出该 Profile 可用的 Tool 子集（Tool 治理的雏形，完整 allow/deny 策略放扩展阶段）。
 */
public class ToolRegistry {

    /**
     * 注册一个 Tool。
     *
     * @param tool 被包装成 AgentOSTool 的工具实例
     */
    public void register(AgentOSTool tool) {
        throw new UnsupportedOperationException("尚未实现：ToolRegistry.register（TS 6.6）");
    }

    /**
     * 按名称查找 Tool。
     *
     * @param name Tool 唯一名称（如 read_file / notify）
     * @return 对应的 AgentOSTool 实例
     */
    public AgentOSTool find(String name) {
        throw new UnsupportedOperationException("尚未实现：ToolRegistry.find（TS 6.6）");
    }
}
