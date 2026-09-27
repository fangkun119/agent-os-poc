package com.agentos.web;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理：把异常转成标准 JSON 响应信封 {@link ApiResponse}（TechnicalSolution.md - 7.1 模块组成），第三周交付（TechnicalSolution.md - 13 实施节奏）。
 *
 * <p>错误码规范：code 为 HTTP 风格状态码字符串（封闭集 {400, 404, 500, 503, 504}，传输语义）；
 * 业务机读分类 errorCode（11 值封闭集）与失败响应形状见 api.md - §4 失败路径契约。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // TODO: 实施阶段补各异常到 ApiResponse 信封的映射（TechnicalSolution.md - 7.1 模块组成 / 7.4 关键设计点）
}
