package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool：read_file（TS 6.2 FileTools 组）。
 *
 * <p>读取指定路径的文件内容。execute 开头先
 * {@code Sandbox.check(new SandboxAction(ActionType.FILE_READ, path))} 做路径白名单校验，
 * 通过才执行真正的 IO；Skill 正文/参考/脚本即经本 Tool 按需读取（TS 6.3 三层渐进式披露）。
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
        throw new UnsupportedOperationException("尚未实现：ReadFileTool.execute（TS 6.2）");
    }
}
