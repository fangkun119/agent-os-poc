package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool：http_post（TS 6.2 HttpTools 组）。
 *
 * <p>对域名白名单内的地址发起 HTTP POST 请求并返回响应。execute 开头先
 * {@code Sandbox.check(new SandboxAction(ActionType.HTTP_REQUEST, url))}，校验通过才执行。
 * 注意 notify.allowed_domains 中的通知渠道域名（webhook URL 内含 token 等同凭证）
 * 不进入本 Tool 可达的 http.allowed_domains 白名单（TS 6.8）。
 */
public class HttpPostTool implements AgentOSTool {

    @Override
    public String getName() {
        return "http_post";
    }

    @Override
    public String getDescription() {
        return "对白名单内域名发起 HTTP POST 请求并返回响应。";
    }

    @Override
    public String getInputSchema() {
        return """
                {"type": "object", "properties": {"url": {"type": "string", "description": "请求地址"}, "body": {"type": "string", "description": "请求体"}}, "required": ["url", "body"]}
                """;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：HttpPostTool.execute（TS 6.2）");
    }
}
