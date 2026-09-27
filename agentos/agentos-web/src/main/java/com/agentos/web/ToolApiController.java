package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tool 信息端点（TechnicalSolution.md - 7.2 核心阶段端点），第三周交付（TechnicalSolution.md - 13 实施节奏）。
 *
 * <p>GET /api/v1/tools：列出 ToolRegistry 中已注册的 Tool 信息（内置九个 + Plugin 三档接入，见 TechnicalSolution.md - 6 核心能力四：Tool 体系）。
 * Tool describe 与调用历史属扩展阶段端点（TechnicalSolution.md - 7.3 扩展阶段补齐的端点），核心阶段只读。
 */
@RestController
public class ToolApiController {

    /** 查 Tool 列表（TechnicalSolution.md - 7.2 核心阶段端点）。 */
    @GetMapping("/api/v1/tools")
    public ApiResponse list() {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }
}
