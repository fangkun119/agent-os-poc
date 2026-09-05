package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profile 查询端点（TS 7.2），第三周交付（TS 13）。
 *
 * <p>GET /api/v1/profiles：列出已加载的 Agent Profile（AGENT.md frontmatter 派生，见 TS 11.1）。
 * 核心阶段只做查询不做创建（TS 7.3）。
 */
@RestController
public class ProfileApiController {

    /** 查 Profile 列表（TS 7.2）。 */
    @GetMapping("/api/v1/profiles")
    public ApiResponse list() {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }
}
