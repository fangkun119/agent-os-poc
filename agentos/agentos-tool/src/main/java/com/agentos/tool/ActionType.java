package com.agentos.tool;

/**
 * Sandbox 受控动作类型（TS 6.7），五值：
 *
 * <ul>
 *   <li>文件读 FILE_READ / 文件写 FILE_WRITE——读写分开，便于未来按读/写分权限；
 *       SandboxChecker.check 将两 case 同路由到 checkFilePath</li>
 *   <li>Shell 命令 SHELL_COMMAND</li>
 *   <li>HTTP 请求 HTTP_REQUEST</li>
 *   <li>通知推送 NOTIFY——出站通知独立动作，走独立的 notify.allowed_domains 白名单（TS 6.8）</li>
 * </ul>
 */
public enum ActionType {

    /** 文件读取。 */
    FILE_READ,

    /** 文件写入。 */
    FILE_WRITE,

    /** Shell 命令执行。 */
    SHELL_COMMAND,

    /** HTTP 请求。 */
    HTTP_REQUEST,

    /** 通知推送（经 NotifyChannelAdapter 出站发送）。 */
    NOTIFY
}
