package com.agentos.tool;

/**
 * Sandbox 校验失败异常（TS 6.7）。
 *
 * <p>任意校验失败抛出本异常，Tool 执行终止；异常信息直接复用 ToolExecutor 已有的失败审计路径
 * 写入 tool_invocations（success=false、error_message），不为 Sandbox 单独新增审计逻辑。
 */
public class SandboxViolationException extends RuntimeException {

    public SandboxViolationException(String message) {
        super(message);
    }
}
