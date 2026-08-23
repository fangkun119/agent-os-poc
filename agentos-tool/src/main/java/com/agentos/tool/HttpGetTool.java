package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool：http_get（TS 6.2 HttpTools 组）。
 *
 * <p>对域名白名单内的地址发起 HTTP GET 请求并返回响应。execute 开头先
 * {@code Sandbox.check(new SandboxAction(ActionType.HTTP_REQUEST, url))}，校验通过才执行。
 */
public class HttpGetTool implements AgentOSTool {

    @Override
    public String getName() {
        return "http_get";
    }

    @Override
    public String getDescription() {
        return "对白名单内域名发起 HTTP GET 请求并返回响应。";
    }

    @Override
    public String getInputSchema() {
        return """
                {"type": "object", "properties": {"url": {"type": "string", "description": "请求地址"}}, "required": ["url"]}
                """;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：HttpGetTool.execute（TS 6.2）");
    }
}
