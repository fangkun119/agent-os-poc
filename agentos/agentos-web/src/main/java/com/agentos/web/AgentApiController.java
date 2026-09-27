package com.agentos.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 无状态调用端点（TechnicalSolution.md - 7.2 核心阶段端点），第三周交付（TechnicalSolution.md - 13 实施节奏）。
 *
 * <p>POST /api/v1/agents/{name}/invoke：按 Agent 名做无状态调用（stateless 短任务，
 * 业务系统同步等返回，见 TechnicalSolution.md - 7.6 业务系统集成场景）。Controller 只做参数校验、响应包装、错误处理，
 * 实际逻辑委托给核心层服务（TechnicalSolution.md - 7.1 模块组成）。
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentApiController {

    /** 无状态调用指定 Agent（TechnicalSolution.md - 7.2 核心阶段端点）。Agent 调用循环总超时默认 300s，超时返回 504（TechnicalSolution.md - 7.4 关键设计点）。 */
    @PostMapping("/{name}/invoke")
    public ApiResponse invoke(@PathVariable("name") String name, @RequestBody String body) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }
}
