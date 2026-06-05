package com.socialogin.module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ModuleApplication {

    public static void main(String[] args) {
        // Spring Boot 애플리케이션을 시작하고, 컴포넌트 스캔/자동설정/내장 서버 실행을 수행합니다.
        SpringApplication.run(ModuleApplication.class, args);
    }

}
