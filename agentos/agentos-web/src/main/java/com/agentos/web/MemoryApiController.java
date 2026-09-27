package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Memory 查询端点（TechnicalSolution.md - 7.2 核心阶段端点），第三周交付（TechnicalSolution.md - 13 实施节奏）。
 *
 * <p>GET /api/v1/memory?agent=X：查询长期记忆（MemoryService 三层门面，见 TechnicalSolution.md - 5.1 模块组成）。
 * agent 必选（缺失→400 INVALID_ARGUMENT），X-User-Id 请求头定用户、缺省 default；
 * 契约细节见 docs/design/detail/api.md。
 * append/clear/search 属扩展阶段端点（TechnicalSolution.md - 7.3 扩展阶段补齐的端点），核心阶段只读。
 */
@RestController
public class MemoryApiController {

    /** 查 Memory 信息（TechnicalSolution.md - 7.2 核心阶段端点）。agent 必选。 */
    @GetMapping("/api/v1/memory")
    public ApiResponse get(@RequestParam("agent") String agent) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }
}
