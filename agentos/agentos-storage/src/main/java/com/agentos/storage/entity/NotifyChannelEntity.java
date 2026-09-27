package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 通知渠道全局注册表，对应 SQLite 表 notify_channels：name/type/url/description（TechnicalSolution.md - 6.8 通知推送/9.2 SQLite 关系型数据）。
 *
 * <p>通知渠道是 SQLite 全局注册表，AGENT.md frontmatter 无 notify_channels 字段。
 * 字段全集见 TechnicalSolution.md - 9.2 SQLite 关系型数据。
 */
@Entity
@Table(name = "notify_channels")
public class NotifyChannelEntity {

    /** 主键：渠道名（TechnicalSolution.md - 6.8 通知推送/9.2 SQLite 关系型数据）。 */
    @Id
    @Column(name = "name")
    private String name;

    /** 渠道类型（TechnicalSolution.md - 6.8 通知推送）。 */
    @Column(name = "type")
    private String type;

    /** 渠道地址（如 webhook URL，TechnicalSolution.md - 6.8 通知推送）。 */
    @Column(name = "url")
    private String url;

    /** 渠道描述（TechnicalSolution.md - 6.8 通知推送）。 */
    @Column(name = "description")
    private String description;
}
