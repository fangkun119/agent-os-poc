package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务管理端点（TechnicalSolution.md - 8.5 定时任务 / 7.2 核心阶段端点），随第四周收尾端点交付（TechnicalSolution.md - 13 实施节奏）。
 *
 * <p>四个管理端点：GET /api/v1/schedules 列任务与状态、GET /{id}/executions 查执行历史、
 * POST /{id}/run 立即执行一次（走 runNow，无视启用状态）、PUT /{id} 启用/停用（TechnicalSolution.md - 8.5 定时任务）。
 * 只做运行控制，不含通过 API 增删改 cron 定义（定义源是 AGENT.md frontmatter 的
 * schedules 字段，改定义需重启，见 TechnicalSolution.md - 8.5 定时任务 / 7.3 扩展阶段补齐的端点）。
 */
@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleApiController {

    /** 列出定时任务与运行状态（TechnicalSolution.md - 8.5 定时任务）。 */
    @GetMapping
    public ApiResponse list() {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 查某任务的执行历史（TechnicalSolution.md - 8.5 定时任务）。 */
    @GetMapping("/{id}/executions")
    public ApiResponse executions(@PathVariable("id") String id) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 立即执行一次（runNow，无视启用状态，TechnicalSolution.md - 8.5 定时任务）。 */
    @PostMapping("/{id}/run")
    public ApiResponse runNow(@PathVariable("id") String id) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 启用/停用任务（TechnicalSolution.md - 8.5 定时任务）。 */
    @PutMapping("/{id}")
    public ApiResponse setEnabled(@PathVariable("id") String id) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }
}
