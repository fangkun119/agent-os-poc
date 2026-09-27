package com.agentos.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知渠道注册 CRUD 端点（TechnicalSolution.md - 6.8 通知推送 / 7.2 核心阶段端点），随第四周收尾端点交付（TechnicalSolution.md - 13 实施节奏）。
 *
 * <p>通知渠道是 SQLite 全局注册表（notify_channels 表），AGENT.md frontmatter 无 notify_channels
 * 字段（TechnicalSolution.md - 6.8 通知推送）。每项包含 name、type、url 和可选 description；GET 列表 / POST 注册 /
 * PUT 更新 / DELETE 删除。核心阶段经 API/Swagger 操作，管理台页面放扩展阶段（TechnicalSolution.md - 6.8 通知推送）。
 * 该信封（ApiResponse）自第三周基础端点交付起即为全端点统一响应结构（TechnicalSolution.md - 7.1 模块组成 / api.md - §0 先看结论）。
 */
@RestController
@RequestMapping("/api/v1/notify-channels")
public class NotifyChannelApiController {

    /** 列出已注册通知渠道（TechnicalSolution.md - 6.8 通知推送）。 */
    @GetMapping
    public ApiResponse list() {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 注册通知渠道（TechnicalSolution.md - 6.8 通知推送）。 */
    @PostMapping
    public ApiResponse register(@RequestBody String body) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 更新通知渠道（TechnicalSolution.md - 6.8 通知推送）。 */
    @PutMapping("/{name}")
    public ApiResponse update(@PathVariable("name") String name, @RequestBody String body) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }

    /** 删除通知渠道（TechnicalSolution.md - 6.8 通知推送）。 */
    @DeleteMapping("/{name}")
    public ApiResponse delete(@PathVariable("name") String name) {
        return ApiResponse.error("501", "尚未实现：骨架（TechnicalSolution.md - 7.2 核心阶段端点）");
    }
}
