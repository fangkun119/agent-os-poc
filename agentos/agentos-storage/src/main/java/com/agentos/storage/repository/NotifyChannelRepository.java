package com.agentos.storage.repository;

import com.agentos.storage.entity.NotifyChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * notify_channels 通知渠道注册表仓储（TechnicalSolution.md - 6.8 通知推送/9.2 SQLite 关系型数据）。骨架空接口。
 */
public interface NotifyChannelRepository extends JpaRepository<NotifyChannelEntity, String> {
}
