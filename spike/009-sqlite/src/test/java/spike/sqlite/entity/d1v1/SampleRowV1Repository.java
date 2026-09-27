package spike.sqlite.entity.d1v1;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * sample_row 表 V1 阶段仓储（001-plan.md - 2.2 目录结构）：D1 基线阶段插入与查询样本行用，骨架空接口。
 * 仓库扫描由 D1V1Config 的 @EnableJpaRepositories("spike.sqlite.entity.d1v1") 圈定，不进全局扫描。
 */
public interface SampleRowV1Repository extends JpaRepository<SampleRowV1, Long> {
}
