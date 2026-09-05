package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Memory 查询端点（TS 7.2），第三周交付（TS 13）。
 *
 * <p>GET /api/v1/memory：查询长期记忆（MemoryService 三层门面，见 TS 5.1）。
 * append/clear/search 属扩展阶段端点（TS 7.3），核心阶段只读。
 */
@RestController
public class MemoryApiController {

    /** 查 Memory 信息（TS 7.2）。 */
    @GetMapping("/api/v1/memory")
    public ApiResponse get() {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }
}
