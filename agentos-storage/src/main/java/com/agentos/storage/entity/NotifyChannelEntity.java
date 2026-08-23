package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 通知渠道全局注册表，对应 SQLite 表 notify_channels：name/type/url/description（TS 6.8/9.2）。
 *
 * <p>通知渠道是 SQLite 全局注册表，AGENT.md frontmatter 无 notify_channels 字段。
 * 字段全集见 TS 9.2。
 */
@Entity
@Table(name = "notify_channels")
public class NotifyChannelEntity {

    /** 主键：渠道名（TS 6.8/9.2）。 */
    @Id
    @Column(name = "name")
    private String name;

    /** 渠道类型（TS 6.8）。 */
    @Column(name = "type")
    private String type;

    /** 渠道地址（如 webhook URL，TS 6.8）。 */
    @Column(name = "url")
    private String url;

    /** 渠道描述（TS 6.8）。 */
    @Column(name = "description")
    private String description;
}
