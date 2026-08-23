package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * 内置 Tool：notify（TS 6.2 NotifyTools 组，详见 TS 6.8）。
 *
 * <p>把消息推送到通知渠道：{@code channel} 参数（必填）是通知渠道的全局注册名，按名引用
 * SQLite notify_channels 全局注册表（经 /api/v1/notify-channels 做 CRUD；AGENT.md frontmatter
 * 无 notify_channels 字段），NotifyTool 从注册表解析适配器与 URL，经 WebhookNotifyAdapter
 * 发送（发送前过 Sandbox.check 的 NOTIFY 独立白名单），具体 webhook 地址不进入对话。
 *
 * <p>内置 Tool 共九个（TS 6.2），本模块提供其中七个；save_memory / recall_memory
 * 两个内置 Tool 归 agentos-memory 模块，不在本模块。
 */
public class NotifyTool implements AgentOSTool {

    @Override
    public String getName() {
        return "notify";
    }

    @Override
    public String getDescription() {
        return "把消息推送到按名引用的注册通知渠道（如企业微信群机器人 webhook）。";
    }

    @Override
    public String getInputSchema() {
        return """
                {"type": "object", "properties": {"channel": {"type": "string", "description": "通知渠道全局注册名（必填）"}, "content": {"type": "string", "description": "通知内容"}}, "required": ["channel", "content"]}
                """;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：NotifyTool.execute（TS 6.8）");
    }
}
