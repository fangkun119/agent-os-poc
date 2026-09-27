package spike.sqlite.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * D1 实验上下文 A（V1 基线）装配入口（001-plan.md - 2.2 目录结构 骨架设计说明 2）。
 *
 * <p>只圈 d1v1 包的实体与仓库。D1 实验以 SpringApplicationBuilder 程序化起停本上下文与
 * D1V2Config 上下文（上下文用后即关）；同表名 sample_row 的两组实体绝不能进同一 Spring 上下文
 * （001-spec.md - 2.1 条款 D1（P0）：ddl-auto=update 对既有表的真实行为）。
 * 实体/仓库在主类包之外时必须显式 @EntityScan / @EnableJpaRepositories
 * （agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）条款）。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("spike.sqlite.entity.d1v1")
@EnableJpaRepositories("spike.sqlite.entity.d1v1")
public class D1V1Config {
}
