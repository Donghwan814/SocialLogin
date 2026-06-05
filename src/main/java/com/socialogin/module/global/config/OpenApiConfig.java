package com.socialogin.module.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 문서 기본 설정입니다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        // SpringDoc이 사용할 OpenAPI 메타데이터 객체를 직접 구성합니다.
        return new OpenAPI()
                // Swagger UI 상단에 표시될 API 제목/버전/설명을 설정합니다.
                .info(new Info()
                        .title("Social Login Module API")
                        .version("v1")
                        .description("Google, Kakao, Naver, Facebook, GitHub OAuth 로그인과 JWT 발급 API 문서입니다."))
                // Swagger UI에서 요청을 보낼 서버 주소를 등록합니다.
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local server")
                ))
                // JWT 인증 API가 생겼을 때 Swagger Authorize 버튼에서 사용할 보안 스키마를 정의합니다.
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                // HTTP Authorization 헤더 기반 인증이라는 뜻입니다.
                                .type(SecurityScheme.Type.HTTP)
                                // Bearer token 형식을 사용합니다.
                                .scheme("bearer")
                                // 토큰 포맷 설명용 값입니다.
                                .bearerFormat("JWT")));
    }
}
