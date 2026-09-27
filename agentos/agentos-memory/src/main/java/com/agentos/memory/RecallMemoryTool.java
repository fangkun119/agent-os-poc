package com.agentos.memory;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool {@code recall_memory}：按关键词检索长期记忆（TechnicalSolution.md - 5.1 模块组成的 MemoryTools / 6.2 内置 Tool）。
 *
 * <p>归 Memory 模块，但作为内置 Tool 注册到 ToolRegistry。检索只在归档记忆区做匹配，
 * 核心区不参与（它本来就会被 load 全量注入）；关键词匹配，不做复杂化（TechnicalSolution.md - 5.1 模块组成的契约四）。
 *
 * <p>sandboxActions 申报空清单：只读检索、无涉外动作可申报
 * （TechnicalSolution.md - 6.7 Sandbox 检查，设计评审 Q5 裁决 2026-09-24）。
 */
public class RecallMemoryTool implements AgentOSTool {

    @Override
    public String getName() {
        return "recall_memory";
    }

    @Override
    public String getDescription() {
        return "按关键词检索长期记忆的归档区，返回匹配的记忆条目。";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "keyword": {
                      "type": "string",
                      "description": "检索关键词，只在归档记忆区匹配"
                    }
                  },
                  "required": ["keyword"]
                }
                """;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        // TODO: 实施阶段补 LongTermMemoryStore.recallByKeyword 调用（TechnicalSolution.md - 5.1 模块组成）
        throw new UnsupportedOperationException("尚未实现：RecallMemoryTool.execute（TechnicalSolution.md - 5.1 模块组成）");
    }
}
