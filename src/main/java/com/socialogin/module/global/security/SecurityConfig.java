package com.socialogin.module.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정 클래스입니다.
 *
 * <p>역할:
 * - REST API에 맞게 session, form login, http basic을 비활성화합니다.
 * - OAuth authorize/login-url/login/callback은 공개하고, 나머지 API는 JWT 인증을 요구합니다.
 * - JwtAuthenticationFilter를 Security filter chain에 등록합니다.
 *
 * <p>프론트엔드가 별도로 있는 구조:
 * - OAuth2 세션 로그인(oauth2Login)은 사용하지 않고 직접 code 교환 REST 흐름을 사용합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // 요청마다 Authorization 헤더의 JWT를 검사하는 커스텀 필터입니다.
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * JWT 기반 stateless 인증에 맞춘 SecurityFilterChain입니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        // HttpSecurity 설정을 체이닝 방식으로 구성하고 최종 SecurityFilterChain Bean으로 반환합니다.
        return http
                // REST API는 쿠키 기반 CSRF 보호보다 Bearer Token 인증을 쓰므로 CSRF를 끕니다.
                .csrf(csrf -> csrf.disable())
                // 프론트엔드 도메인에서 API를 호출할 수 있게 CORS 설정을 연결합니다.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // 서버 세션을 만들지 않는 stateless JWT 인증 구조로 설정합니다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // HTML form 로그인 화면을 사용하지 않으므로 비활성화합니다.
                .formLogin(form -> form.disable())
                // 브라우저 basic auth 팝업을 사용하지 않으므로 비활성화합니다.
                .httpBasic(httpBasic -> httpBasic.disable())
                // Spring Security 기본 OAuth2 세션 로그인을 쓰지 않고 직접 REST OAuth 흐름을 구현하므로 비활성화합니다.
                .oauth2Login(oauth2Login -> oauth2Login.disable())
                // URL별 접근 권한을 설정합니다.
                .authorizeHttpRequests(auth -> auth
                        // 로그인 URL 조회는 로그인 전 호출해야 하므로 공개합니다.
                        .requestMatchers(HttpMethod.GET, "/api/auth/oauth/*/login-url").permitAll()
                        // provider 로그인 페이지 redirect도 로그인 전 접근해야 하므로 공개합니다.
                        .requestMatchers(HttpMethod.GET, "/api/auth/oauth/*/authorize").permitAll()
                        // 기존 GET /login redirect 호환 endpoint도 공개합니다.
                        .requestMatchers(HttpMethod.GET, "/api/auth/oauth/*/login").permitAll()
                        // 프론트엔드가 authorization code를 전달해 JWT를 발급받는 API입니다.
                        .requestMatchers(HttpMethod.POST, "/api/auth/oauth/*/login").permitAll()
                        // legacy provider callback은 provider가 호출할 수 있으므로 공개하되 JWT는 발급하지 않습니다.
                        .requestMatchers(HttpMethod.GET, "/api/auth/oauth/*/callback").permitAll()
                        // Swagger UI HTML 진입점을 공개합니다.
                        .requestMatchers("/swagger-ui.html").permitAll()
                        // Swagger 정적 리소스와 OpenAPI JSON을 공개합니다.
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 위에서 열어둔 URL 외 모든 API는 인증이 필요합니다.
                        .anyRequest().authenticated()
                )
                // UsernamePasswordAuthenticationFilter 전에 JWT 필터를 실행해 SecurityContext를 먼저 채웁니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 설정을 실제 SecurityFilterChain 객체로 빌드합니다.
                .build();
    }

    /**
     * CORS 설정입니다.
     *
     * <p>실무에서는 allowed-origins를 운영 도메인으로 제한해야 하며, credentials=true와 wildcard(*)를 함께 쓰지 않습니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:8080}") String allowedOrigins
    ) {
        // Spring CORS 설정 객체를 생성합니다.
        CorsConfiguration configuration = new CorsConfiguration();

        // 쉼표 문자열로 받은 허용 origin을 List로 바꿔 설정합니다.
        configuration.setAllowedOrigins(splitCsv(allowedOrigins));

        // 프론트엔드가 사용할 수 있는 HTTP method 목록입니다.
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Authorization은 JWT 전달에, Content-Type은 JSON body 전달에 필요합니다.
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // 쿠키/인증 정보 포함 요청을 허용합니다. wildcard origin과 함께 쓰면 안 됩니다.
        configuration.setAllowCredentials(true);

        // preflight 결과를 3600초 동안 브라우저가 캐시하게 합니다.
        configuration.setMaxAge(3600L);

        // URL pattern별 CORS 설정을 담는 source입니다.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 모든 경로에 위 CORS 설정을 적용합니다.
        source.registerCorsConfiguration("/**", configuration);

        // SecurityFilterChain에서 사용할 CORS 설정 source를 반환합니다.
        return source;
    }

    /**
     * 쉼표로 구분된 origin 설정을 List로 변환합니다.
     */
    private List<String> splitCsv(String value) {
        // "a,b,c" 문자열을 Stream으로 분리합니다.
        return Arrays.stream(value.split(","))
                // 각 origin 앞뒤 공백을 제거합니다.
                .map(String::trim)
                // 빈 값은 제거합니다.
                .filter(origin -> !origin.isBlank())
                // 최종 List로 만듭니다.
                .toList();
    }
}
