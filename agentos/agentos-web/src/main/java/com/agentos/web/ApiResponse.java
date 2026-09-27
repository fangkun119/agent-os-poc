package com.agentos.web;

import java.time.Instant;

/**
 * 标准响应信封：code、message、data、timestamp，成功与错误共用一个信封（TechnicalSolution.md - 7.1 模块组成）。
 *
 * <p>全部端点自第三周交付起，成功与错误响应均包此信封（权威口径：api.md - §0 先看结论 第 1 条）；
 * {@code GlobalExceptionHandler} 第三周交付时即采用同一结构返回错误，后续复用、不另建 ErrorBody（TechnicalSolution.md - 7.1 模块组成）。
 *
 * @param code      HTTP 风格状态码字符串（传输语义，成功固定 "200"；错误码规范见 api.md - §4 失败路径契约）
 * @param message   人类可读消息
 * @param data      业务负载；错误时 data 恒为三字段对象 {errorCode, sessionId, lastTermination}
 *                  （契约见 api.md - §4 失败路径契约），仅骨架 501 占位响应例外（见两参 error 工厂）
 * @param timestamp 响应生成时间
 */
public record ApiResponse(String code, String message, Object data, Instant timestamp) {

    /** 成功响应工厂（TechnicalSolution.md - 7.1 模块组成）。 */
    public static ApiResponse ok(Object data) {
        return new ApiResponse("200", "OK", data, Instant.now());
    }

    /**
     * 骨架 501 占位专用错误工厂（非契约路径）：data 为 null，仅骨架期占位方法使用。
     * 契约错误响应必须用三参重载并构造三字段 data（{errorCode, sessionId, lastTermination}，
     * 错误码封闭集见 api.md - §4 失败路径契约）。
     */
    public static ApiResponse error(String code, String message) {
        return error(code, message, null);
    }

    /** 契约错误响应工厂：data 恒为三字段对象 {errorCode, sessionId, lastTermination}（错误码封闭集见 api.md - §4 失败路径契约）。 */
    public static ApiResponse error(String code, String message, Object data) {
        return new ApiResponse(code, message, data, Instant.now());
    }
}
