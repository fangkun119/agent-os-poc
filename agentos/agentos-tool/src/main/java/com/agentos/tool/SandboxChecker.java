package com.agentos.tool;

/**
 * Sandbox 核心阶段唯一实现（TS 6.7）：应用层 Path/Pattern 白名单（劝阻级防线，
 * 防模型犯傻误操作，防不住蓄意绕过）。
 *
 * <p>白名单四项配置（application.yaml）：file.allowed_paths / shell.allowed_commands /
 * http.allowed_domains / notify.allowed_domains。notify 独立白名单，不复用
 * http.allowed_domains——通知渠道域名（webhook URL 内含 token 等同凭证）不进入通用 HTTP 白名单。
 *
 * <p>check 按 ActionType 路由到四个私有校验方法，FILE_READ、FILE_WRITE 两 case 同路由到
 * {@link #checkFilePath}。
 *
 * <p>解释器警示：参数不校验——解释器一旦列入白名单即视为放通其全部文件/网络行为，
 * 文件白名单对其不生效；列入解释器属高危运维决策。
 */
public class SandboxChecker implements Sandbox {

    @Override
    public void check(SandboxAction action) {
        throw new UnsupportedOperationException("尚未实现：SandboxChecker.check（TS 6.7）");
    }

    /** 路径标准化后比对 file.allowed_paths 白名单，需处理 {@code ../} 路径穿越（TS 6.7）。 */
    private void checkFilePath(String path) {
        // TODO: 路径标准化后比对白名单，处理 ../ 路径穿越
    }

    /** 精确比对 shell.allowed_commands 可执行文件白名单；解释器仅在管理员显式列入时允许（TS 6.7）。 */
    private void checkShellCommand(String command) {
        // TODO: 精确比对可执行文件白名单
    }

    /** 解析 host 后与 http.allowed_domains 做通配符匹配（TS 6.7）。 */
    private void checkHttpUrl(String url) {
        // TODO: 解析 host 后做通配符匹配
    }

    /** 校验独立的 notify.allowed_domains，不复用 http.allowed_domains（TS 6.7）。 */
    private void checkNotifyUrl(String url) {
        // TODO: 校验 notify 独立白名单
    }
}
