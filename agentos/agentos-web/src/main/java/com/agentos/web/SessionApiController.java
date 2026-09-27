package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话管理端点（TechnicalSolution.md - 7.2 核心阶段端点），第三周交付（TechnicalSolution.md - 13 实施节奏）。
 *
 * <p>四个端点：POST /api/v1/sessions（创建）、POST /{id}/messages（发消息）、
 * GET /{id}（单查，messages 全量无上限不分页，分页在扩展阶段）、GET（列表，cnt 缺省 100、上限 1000）。
 * 契约细节见 docs/design/detail/api.md。
 * Controller 只做参数校验、响应包装、错误处理，实际逻辑委托给核心层服务（TechnicalSolution.md - 7.1 模块组成）。
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionApiController {

    /** 创建会话（TechnicalSolution.md - 7.2 核心阶段端点）。 */
    @PostMapping
    public ApiResponse create(@RequestBody String body) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 向会话发消息（TechnicalSolution.md - 7.2 核心阶段端点）。单条消息最大 32KB（TechnicalSolution.md - 7.4 关键设计点）。 */
    @PostMapping("/{id}/messages")
    public ApiResponse sendMessage(@PathVariable("id") String id, @RequestBody String body) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 单查会话（TechnicalSolution.md - 7.2 核心阶段端点）。messages 全量返回、无上限不分页（分页在扩展阶段）。 */
    @GetMapping("/{id}")
    public ApiResponse history(@PathVariable("id") String id) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 会话列表（TechnicalSolution.md - 7.2 核心阶段端点）。cnt 缺省 100、上限 1000，按 lastActiveAt 倒序。 */
    @GetMapping
    public ApiResponse list(
            @RequestParam(name = "cnt", defaultValue = "100") int cnt) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }
}
