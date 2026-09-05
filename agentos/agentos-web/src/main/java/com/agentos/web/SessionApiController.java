package com.agentos.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话管理端点（TS 7.2），第三周交付（TS 13）。
 *
 * <p>四个端点：POST /api/v1/sessions（创建）、POST /{id}/messages（发消息）、
 * GET /{id}（查历史，最多返回最近 100 条，见 TS 7.4）、DELETE /{id}（归档）。
 * Controller 只做参数校验、响应包装、错误处理，实际逻辑委托给核心层服务（TS 7.1）。
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionApiController {

    /** 创建会话（TS 7.2）。 */
    @PostMapping
    public ApiResponse create(@RequestBody String body) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 向会话发消息（TS 7.2）。单条消息最大 32KB（TS 7.4）。 */
    @PostMapping("/{id}/messages")
    public ApiResponse sendMessage(@PathVariable("id") String id, @RequestBody String body) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 查会话历史（TS 7.2）。最多返回最近 100 条（TS 7.4）。 */
    @GetMapping("/{id}")
    public ApiResponse history(@PathVariable("id") String id) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }

    /** 归档会话（TS 7.2）。 */
    @DeleteMapping("/{id}")
    public ApiResponse archive(@PathVariable("id") String id) {
        return ApiResponse.error("501", "尚未实现：骨架（TS 7.2）");
    }
}
