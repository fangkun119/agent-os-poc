package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool：read_file（TechnicalSolution.md - 6.2 内置 Tool 的 FileTools 组）。
 *
 * <p>读取指定路径的文件内容。execute 开头先
 * {@code Sandbox.check(new SandboxAction(ActionType.FILE_READ, path))} 做路径白名单校验，
 * 通过才执行真正的 IO；Skill 正文/参考/脚本即经本 Tool 按需读取（TechnicalSolution.md - 6.3 Plugin Tool 方式一 的三层渐进式披露）。
 */
public class ReadFileTool implements AgentOSTool {

    @Override
    public String getName() {
        return "read_file";
    }

    @Override
    public String getDescription() {
        return "读取指定路径的文本文件内容。";
    }

    @Override
    public String getInputSchema() {
        return """
                {"type": "object", "properties": {"path": {"type": "string", "description": "文件路径"}}, "required": ["path"]}
                """;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：ReadFileTool.execute（TechnicalSolution.md - 6.2 内置 Tool）");
    }
}
