package spike.reactloop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 自含启动类（方案 B：第一组 SpikeApp 已随 src/ 归档，见 002-spec §3.0）。
 * 无业务逻辑，供各测试类启动 Spring 容器。
 */
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
