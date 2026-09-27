package spike.sqlite.entity.d4;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * type_probe 表仓储（001-plan.md - 2.2 目录结构）：D4 的 CRUD 全链与类型往返实验用，骨架空接口。
 * 实体/仓库由 D4 测试类的内嵌配置显式圈包（001-plan.md - 2.2 目录结构 骨架设计说明 2）。
 */
public interface TypeProbeRepository extends JpaRepository<TypeProbe, Long> {
}
