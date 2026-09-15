package spike.mcp.tool;

/**
 * 工具执行结果统一结构（TS 6.1 四要素；002 §3.4）。
 *
 * @param success   成功标识
 * @param content   结果内容（成功时为工具产出；多段 content 按 002 §3.5 拼接）
 * @param error     错误信息（失败时非空；注意脱敏纪律——不完整打印凭证类内容）
 * @param retryable 是否可重试（初值判定见 002 开放项 6，M4/M6 实测定案）
 */
public record ToolResult(boolean success, String content, String error, boolean retryable) {

    /** 成功结果工厂 */
    public static ToolResult ok(String content) {
        return new ToolResult(true, content, null, false);
    }

    /** 失败结果工厂：连接 / 超时类默认可重试 */
    public static ToolResult retryableFailure(String error) {
        return new ToolResult(false, null, error, true);
    }

    /** 失败结果工厂：参数 / 不存在类默认不可重试 */
    public static ToolResult permanentFailure(String error) {
        return new ToolResult(false, null, error, false);
    }
}
