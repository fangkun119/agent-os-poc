package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool：write_file（TS 6.2 FileTools 组）。
 *
 * <p>向指定路径写入内容。execute 开头先
 * {@code Sandbox.check(new SandboxAction(ActionType.FILE_WRITE, path))} 做路径白名单校验，
 * 通过才执行真正的 IO。
 */
public class WriteFileTool implements AgentOSTool {

    @Override
    public String getName() {
        return "write_file";
    }

    @Override
    public String getDescription() {
        return "向指定路径写入文本内容。";
    }

    @Override
    public String getInputSchema() {
        return """
                {"type": "object", "properties": {"path": {"type": "string", "description": "文件路径"}, "content": {"type": "string", "description": "写入内容"}}, "required": ["path", "content"]}
                """;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：WriteFileTool.execute（TS 6.2）");
    }
}
