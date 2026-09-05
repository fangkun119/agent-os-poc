package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool：list_dir（TS 6.2 FileTools 组）。
 *
 * <p>列出指定目录的内容。execute 开头先 {@code Sandbox.check(...)}（FILE_READ / FILE_WRITE
 * 同路由 checkFilePath，TS 6.7）做路径白名单校验，通过才执行真正的 IO。
 */
public class ListDirTool implements AgentOSTool {

    @Override
    public String getName() {
        return "list_dir";
    }

    @Override
    public String getDescription() {
        return "列出指定目录下的文件与子目录。";
    }

    @Override
    public String getInputSchema() {
        return """
                {"type": "object", "properties": {"path": {"type": "string", "description": "目录路径"}}, "required": ["path"]}
                """;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：ListDirTool.execute（TS 6.2）");
    }
}
