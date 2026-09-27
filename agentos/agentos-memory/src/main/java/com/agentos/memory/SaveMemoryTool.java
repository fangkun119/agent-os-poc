package com.agentos.memory;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool {@code save_memory}：把内容追加到长期记忆（TechnicalSolution.md - 5.1 模块组成的 MemoryTools / 6.2 内置 Tool）。
 *
 * <p>归 Memory 模块，但作为内置 Tool 注册到 ToolRegistry，跟其他内置 Tool 一视同仁。
 * 分区由 Agent 经 {@code scope} 参数显式指定（CORE / ARCHIVAL，默认 ARCHIVAL），系统不猜；
 * 核心阶段不做自动抽取，写入时机和 scope 完全由 Agent 手动决定（TechnicalSolution.md - 5.1 模块组成）。
 *
 * <p>sandboxActions 申报空清单：记忆读写无涉外动作可申报——存储路径由底座按
 * {@code <Agent, user>} 身份派生、模型入参（content/scope）不含路径面
 * （TechnicalSolution.md - 6.7 Sandbox 检查，设计评审 Q5 裁决 2026-09-24）。
 */
public class SaveMemoryTool implements AgentOSTool {

    @Override
    public String getName() {
        return "save_memory";
    }

    @Override
    public String getDescription() {
        return "追加一条长期记忆：scope 取 CORE（永不截断、全量注入）或 ARCHIVAL（默认，可被检索），由调用方显式指定。";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "content": {
                      "type": "string",
                      "description": "要写入长期记忆的内容"
                    },
                    "scope": {
                      "type": "string",
                      "enum": ["CORE", "ARCHIVAL"],
                      "description": "目标分区，默认 ARCHIVAL",
                      "default": "ARCHIVAL"
                    }
                  },
                  "required": ["content"]
                }
                """;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        // TODO: 实施阶段补 LongTermMemoryStore.append 调用（TechnicalSolution.md - 5.1 模块组成）
        throw new UnsupportedOperationException("尚未实现：SaveMemoryTool.execute（TechnicalSolution.md - 5.1 模块组成）");
    }
}
