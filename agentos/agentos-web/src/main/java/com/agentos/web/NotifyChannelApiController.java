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
 * 通知渠道注册 CRUD 端点（TS 6.8 / 7.2），随第四周收尾端点交付（TS 13）。
 *
 * <p>通知渠道是 SQLite 全局注册表（notify_channels 表），AGENT.md frontmatter 无 notify_channels
 * 字段（TS 6.8）。每项包含 name、type、url 和可选 description；GET 列表 / POST 注册 /
 * PUT 更新 / DELETE 删除。核心阶段经 API/Swagger 操作，管理台页面放扩展阶段（TS 6.8）。
 * 该信封（ApiResponse）即随本组首个管理端点引入（TS 7.1）。
 */
@RestController
@RequestMapping("/api/v1/notify-channels")
public class NotifyChannelApiController {

    /** 列出已注册通知渠道（TS 6.8）。 */
    @GetMapping
    public ApiResponse list() {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 注册通知渠道（TS 6.8）。 */
    @PostMapping
    public ApiResponse register(@RequestBody String body) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 更新通知渠道（TS 6.8）。 */
    @PutMapping("/{name}")
    public ApiResponse update(@PathVariable("name") String name, @RequestBody String body) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 删除通知渠道（TS 6.8）。 */
    @DeleteMapping("/{name}")
    public ApiResponse delete(@PathVariable("name") String name) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }
}
