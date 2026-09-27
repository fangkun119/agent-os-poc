package spike.sqlite.entity.d4;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * D4 实体：表 type_probe，一个实体覆盖四类字段 Boolean / long / String / LocalDateTime
 * （001-spec.md - 2.4 条款 D4（P0）：社区方言常规 JPA 面够用性 的类型映射条款）。
 *
 * <p>新表三件套齐（id、create_time、update_time；agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）新表条款）；
 * flag_value 用包装类型 Boolean、count_value 按字段清单用基本类型 long（001-plan.md - 2.2 目录结构），
 * 两种形态同表对照方言的列声明与往返映射。
 */
@Entity
@Table(name = "type_probe")
public class TypeProbe {

    /** 主键：数据库自增（GenerationType.IDENTITY；agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）SQLite 主键条款）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 四类字段之一：布尔（包装类型）。 */
    @Column(name = "flag_value")
    private Boolean flagValue;

    /** 四类字段之一：整型（基本类型 long）。 */
    @Column(name = "count_value")
    private long countValue;

    /** 四类字段之一：字符串。 */
    @Column(name = "name_value")
    private String nameValue;

    /** 四类字段之一：时间（LocalDateTime，含纳秒精度与跨日时刻两组边界样本的往返对象）。 */
    @Column(name = "event_time")
    private LocalDateTime eventTime;

    /** 行创建时刻（新表三件套之一；agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）新表条款）。 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 行更新时刻（新表三件套之一；任何更新路径必须同步刷新本列，同上条款）。 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getFlagValue() {
        return flagValue;
    }

    public void setFlagValue(Boolean flagValue) {
        this.flagValue = flagValue;
    }

    public long getCountValue() {
        return countValue;
    }

    public void setCountValue(long countValue) {
        this.countValue = countValue;
    }

    public String getNameValue() {
        return nameValue;
    }

    public void setNameValue(String nameValue) {
        this.nameValue = nameValue;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
