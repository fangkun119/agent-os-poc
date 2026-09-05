package com.agentos.tool;

/**
 * Sandbox 受控动作（TS 6.7）：动作类型 + 目标字符串（路径 / 命令 / URL）。
 *
 * <pre>{@code
 * SandboxAction = { type: ActionType, target: String }
 * }</pre>
 */
public record SandboxAction(ActionType type, String target) {
}
