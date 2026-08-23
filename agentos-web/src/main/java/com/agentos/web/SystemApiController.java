package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统状态端点（TS 7.2），第三周交付（TS 13）。
 *
 * <p>GET /api/v1/health 健康检查、GET /api/v1/info 系统信息（版本等）。
 * 核心阶段无认证（内网假设，见 TS 7.5）。
 */
@RestController
public class SystemApiController {

    /** 健康检查（TS 7.2）。 */
    @GetMapping("/api/v1/health")
    public ApiResponse health() {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 系统信息（TS 7.2）。 */
    @GetMapping("/api/v1/info")
    public ApiResponse info() {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }
}
