package com.agentos.web;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理：把异常转成标准 JSON 响应信封 {@link ApiResponse}（TS 7.1），第三周交付（TS 13）。
 *
 * <p>错误码规范（TS 7.4）：标准 HTTP 状态码加内部错误码——400 参数错误、404 资源不存在、
 * 500 内部错误、503 Provider 故障、504 超 Agent 调用循环总超时（分步预算不硬编码，见 TS 7.4）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // TODO: 实施阶段补各异常到 ApiResponse 信封的映射（TS 7.1 / 7.4）
}
