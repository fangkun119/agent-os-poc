package spike.sqlite.entity.d1v2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * D1 演进实体：表 sample_row 的 V2 形态 = V1 三列（id / name / quantity）不动，只加 remark 列
 * （001-spec.md - 2.1 条款 D1（P0）：ddl-auto=update 对既有表的真实行为 的"实体只加一个字段、不删不改旧列"场景）。
 *
 * <p>D1 实验用本实体在同一库文件上二次启动（演进路径），并另用全新库文件做 V2 首建对照。
 * 本实体与 SampleRowV1 同表名 sample_row，两者绝不能进同一 Spring 上下文——由 D1V2Config 与
 * D1V1Config 各自 @EntityScan / @EnableJpaRepositories 圈包隔离（001-plan.md - 2.2 目录结构 骨架设计说明 2）。
 */
@Entity
@Table(name = "sample_row")
public class SampleRowV2 {

    /** 主键：数据库自增（GenerationType.IDENTITY；agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）SQLite 主键条款）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 样本字符串列（与 V1 同名同列，演进前后不动）。 */
    @Column(name = "name")
    private String name;

    /** 样本整型列（与 V1 同名同列，演进前后不动）。 */
    @Column(name = "quantity")
    private Long quantity;

    /** V2 新增列：D1 观察 ddl-auto=update 是否为既有表自动加列的目标列。 */
    @Column(name = "remark")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
