package spike.sqlite.entity.d1v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * D1 基线实体：表 sample_row 的 V1 形态（001-spec.md - 2.1 条款 D1（P0）：ddl-auto=update 对既有表的真实行为）。
 *
 * <p>只有三列：id（IDENTITY 自增）+ name + quantity。D1 实验先以本实体在库文件上建表并插入样本行，
 * 再换同表名的 SampleRowV2（V1 三列不动、只加 remark 列）二次启动，观察 ddl-auto=update 的真实行为。
 * 本实体与 SampleRowV2 同表名 sample_row，两者绝不能进同一 Spring 上下文——由 D1V1Config 与
 * D1V2Config 各自 @EntityScan / @EnableJpaRepositories 圈包隔离（001-plan.md - 2.2 目录结构 骨架设计说明 2）。
 */
@Entity
@Table(name = "sample_row")
public class SampleRowV1 {

    /** 主键：数据库自增（GenerationType.IDENTITY；agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）SQLite 主键条款）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 样本字符串列。 */
    @Column(name = "name")
    private String name;

    /** 样本整型列（包装类型）。 */
    @Column(name = "quantity")
    private Long quantity;

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
}
