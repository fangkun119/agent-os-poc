package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统状态端点（TechnicalSolution.md - 7.2 核心阶段端点），第三周交付（TechnicalSolution.md - 13 实施节奏）。
 *
 * <p>GET /api/v1/health 健康检查、GET /api/v1/info 系统信息（版本等）。
 * 核心阶段无认证（内网假设，见 TechnicalSolution.md - 7.5 核心阶段不做的部分）。
 */
@RestController
public class SystemApiController {

    /** 健康检查（TechnicalSolution.md - 7.2 核心阶段端点）。 */
    @GetMapping("/api/v1/health")
    public ApiResponse health() {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 系统信息（TechnicalSolution.md - 7.2 核心阶段端点）。 */
    @GetMapping("/api/v1/info")
    public ApiResponse info() {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }
}
