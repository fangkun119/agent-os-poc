package com.agentos.web;

import java.time.Instant;

/**
 * 标准响应信封：code、message、data、timestamp，成功与错误共用一个信封（TS 7.1）。
 *
 * <p>信封随首个管理端点（/api/v1/notify-channels，第四周收尾）引入，
 * {@code GlobalExceptionHandler} 第三周交付时即采用同一信封结构返回错误，后续复用、不另建 ErrorBody（TS 7.1）。
 *
 * @param code      内部错误码（成功固定 "200"；错误码规范见 TS 7.4）
 * @param message   人类可读消息
 * @param data      业务负载，错误时可为 {@code null}
 * @param timestamp 响应生成时间
 */
public record ApiResponse(String code, String message, Object data, Instant timestamp) {

    /** 成功响应工厂（TS 7.1）。 */
    public static ApiResponse ok(Object data) {
        return new ApiResponse("200", "OK", data, Instant.now());
    }

    /** 错误响应工厂，错误码规范 400/404/500/503/504（TS 7.4）。 */
    public static ApiResponse error(String code, String message) {
        return new ApiResponse(code, message, null, Instant.now());
    }
}
