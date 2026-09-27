package spike.sqlite.entity.d1v2;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * sample_row 表 V2 阶段仓储（001-plan.md - 2.2 目录结构）：D1 演进与首建对照阶段插入、查询与行数统计用，骨架空接口。
 * 仓库扫描由 D1V2Config 的 @EnableJpaRepositories("spike.sqlite.entity.d1v2") 圈定，不进全局扫描。
 */
public interface SampleRowV2Repository extends JpaRepository<SampleRowV2, Long> {
}
