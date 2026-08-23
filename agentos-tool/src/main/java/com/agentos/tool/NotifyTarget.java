package com.agentos.tool;

import java.util.Map;

/**
 * 通知目标（TS 6.8）：渠道类型 + 渠道配置。
 *
 * <pre>{@code
 * NotifyTarget = { channelType: String, config: Map<String, String> }
 * }</pre>
 */
public record NotifyTarget(String channelType, Map<String, String> config) {
}
