package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profile 查询端点（TechnicalSolution.md - 7.2 核心阶段端点），第三周交付（TechnicalSolution.md - 13 实施节奏）。
 *
 * <p>GET /api/v1/profiles：列出已加载的 Agent Profile（AGENT.md frontmatter 派生，见 TechnicalSolution.md - 11.1 术语：一个目录 = 一个 Agent）。
 * 核心阶段不提供 Agent 定义类写端点（TechnicalSolution.md - 7.3 扩展阶段补齐的端点），本资源只读；契约见 docs/design/detail/api.md。
 */
@RestController
public class ProfileApiController {

    /** 查 Profile 列表（TechnicalSolution.md - 7.2 核心阶段端点）。 */
    @GetMapping("/api/v1/profiles")
    public ApiResponse list() {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }
}
