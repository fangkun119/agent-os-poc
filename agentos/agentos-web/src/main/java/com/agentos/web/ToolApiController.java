package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tool 信息端点（TS 7.2），第三周交付（TS 13）。
 *
 * <p>GET /api/v1/tools：列出 ToolRegistry 中已注册的 Tool 信息（内置九个 + Plugin 三档接入，见 TS 6）。
 * Tool describe 与调用历史属扩展阶段端点（TS 7.3），核心阶段只读。
 */
@RestController
public class ToolApiController {

    /** 查 Tool 列表（TS 7.2）。 */
    @GetMapping("/api/v1/tools")
    public ApiResponse list() {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }
}
