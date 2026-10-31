package com.campusbook;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.campusbook.mapper")
public class CampusBookApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusBookApplication.class, args);
        System.out.println("""

                ==========================================================
                  校园二手书供需匹配与智能定价系统  服务端已启动
                  API 文档: http://localhost:8080/swagger-ui.html
                ==========================================================
                """);
    }
}
