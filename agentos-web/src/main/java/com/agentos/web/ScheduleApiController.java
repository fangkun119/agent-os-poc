package com.agentos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务管理端点（TS 8.5 / 7.2），随第四周收尾端点交付（TS 13）。
 *
 * <p>四个管理端点：GET /api/v1/schedules 列任务与状态、GET /{id}/executions 查执行历史、
 * POST /{id}/run 立即执行一次（走 runNow，无视启用状态）、PUT /{id} 启用/停用（TS 8.5）。
 * 只做运行控制，不含通过 API 增删改 cron 定义（定义源是 AGENT.md frontmatter 的
 * schedules 字段，改定义需重启，见 TS 8.5 / 7.3）。
 */
@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleApiController {

    /** 列出定时任务与运行状态（TS 8.5）。 */
    @GetMapping
    public ApiResponse list() {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 查某任务的执行历史（TS 8.5）。 */
    @GetMapping("/{id}/executions")
    public ApiResponse executions(@PathVariable("id") String id) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 立即执行一次（runNow，无视启用状态，TS 8.5）。 */
    @PostMapping("/{id}/run")
    public ApiResponse runNow(@PathVariable("id") String id) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 启用/停用任务（TS 8.5）。 */
    @PutMapping("/{id}")
    public ApiResponse setEnabled(@PathVariable("id") String id) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }
}
