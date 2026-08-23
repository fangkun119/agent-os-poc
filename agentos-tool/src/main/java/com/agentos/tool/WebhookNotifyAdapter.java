package com.agentos.tool;

/**
 * 通知核心阶段唯一实现（TS 6.8）：通用 HTTP webhook。
 *
 * <p>用通用 HTTP webhook 承接所有场景——企业微信、飞书、钉钉的群机器人都提供 webhook 地址，
 * 核心阶段不逐家接专用 API（签名算法、AccessToken 刷新这些认证细节核心阶段不做），直接把
 * content 包成对方 webhook 约定的 JSON 格式发一次 POST。
 *
 * <p>发送前过 {@code Sandbox.check(new SandboxAction(ActionType.NOTIFY, url))}，校验独立的
 * notify.allowed_domains 白名单——通知渠道域名（webhook URL 内含 token 等同凭证）不进入通用
 * HTTP 白名单，http_post 打不到它们。
 */
public class WebhookNotifyAdapter implements NotifyChannelAdapter {

    @Override
    public void send(NotifyTarget target, String content) {
        throw new UnsupportedOperationException("尚未实现：WebhookNotifyAdapter.send（TS 6.8）");
    }
}
