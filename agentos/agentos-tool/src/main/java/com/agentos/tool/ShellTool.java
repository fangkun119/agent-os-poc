package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool：shell（TS 6.2 ShellTools 组）。
 *
 * <p>直接执行白名单内的可执行文件与参数数组，带超时，不经 Shell 解释。execute 开头先
 * {@code Sandbox.check(new SandboxAction(ActionType.SHELL_COMMAND, command))}，
 * 校验通过才执行。
 */
public class ShellTool implements AgentOSTool {

    @Override
    public String getName() {
        return "shell";
    }

    @Override
    public String getDescription() {
        return "执行白名单内的可执行文件与参数数组（不经 Shell 解释）。";
    }

    @Override
    public String getInputSchema() {
        return """
                {"type": "object", "properties": {"command": {"type": "array", "items": {"type": "string"}, "description": "可执行文件与参数数组"}}, "required": ["command"]}
                """;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：ShellTool.execute（TS 6.2）");
    }
}
