package spike.sqlite.entity.d3;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * spike_message 表仓储（001-plan.md - 2.2 目录结构）：D3 查询行数与行清单用，骨架空接口。
 * 实体/仓库由 D3 测试类的内嵌配置显式圈包（001-plan.md - 2.2 目录结构 骨架设计说明 2）。
 */
public interface SpikeMessageRepository extends JpaRepository<SpikeMessage, Long> {
}
