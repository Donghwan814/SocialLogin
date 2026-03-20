# 🔐 Spring Boot Social Login Module

> Google · Kakao · Naver OAuth2 소셜 로그인 통합 모듈

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen?logo=springboot)
![OAuth2](https://img.shields.io/badge/OAuth2-Social%20Login-blue)
![JWT](https://img.shields.io/badge/JWT-jjwt-purple)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-Latest-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 📌 목차

- [프로젝트 소개](#-프로젝트-소개)
- [지원 플랫폼](#-지원-플랫폼)
- [기술 스택](#-기술-스택)
- [시작하기](#-시작하기)
    - [사전 요구사항](#사전-요구사항)
    - [소셜 앱 등록](#소셜-앱-등록)
    - [환경 변수 설정](#환경-변수-설정)
    - [실행 방법](#실행-방법)
- [프로젝트 구조](#-프로젝트-구조)
- [API 명세](#-api-명세)
- [인증 플로우](#-인증-플로우)
- [주요 설정](#-주요-설정)
- [테스트](#-테스트)
- [트러블슈팅](#-트러블슈팅)
- [기여 방법](#-기여-방법)
- [라이선스](#-라이선스)

---

## 📖 프로젝트 소개

Spring Boot 4와 Spring Security OAuth2 Client를 기반으로 구글, 카카오, 네이버 소셜 로그인을 손쉽게 연동할 수 있는 **재사용 가능한 인증 모듈**입니다.

- **domain 패키지**: 각 엔티티(도메인) 단위로 기능을 묶어 응집도 있게 관리
- **global 패키지**: 공통 예외처리, Security 설정, Swagger, JWT, BaseEntity 등 프로젝트 전역에서 사용되는 공통 요소 관리
- 신규 회원은 소셜 로그인 최초 시도 시 자동으로 가입 처리
- JWT 기반의 Stateless 인증 방식 적용

---

## 🌐 지원 플랫폼

| 플랫폼 | 지원 여부 | 비고 |
|--------|----------|------|
| Google | ✅ | Spring Security 기본 제공 |
| Kakao  | ✅ | 커스텀 Provider 구현 |
| Naver  | ✅ | 커스텀 Provider 구현 |

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Web | Spring Web MVC |
| Security | Spring Security, OAuth2 Client |
| Auth | JWT (jjwt) |
| ORM | Spring Data JPA |
| DB | MySQL 8.x |
| 편의 | Lombok |
| Docs | Swagger (SpringDoc OpenAPI) |
| Build | Gradle |
| Test | JUnit Platform, Spring Boot Test Slices |

---

## 🚀 시작하기

### 사전 요구사항

- Java 21 이상
- Gradle 8.x 이상
- MySQL 8.x 실행 중
- 각 플랫폼별 OAuth2 앱 등록 완료

---

### 소셜 앱 등록

#### Google
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. **API 및 서비스 > 사용자 인증 정보 > OAuth 2.0 클라이언트 ID** 생성
3. 승인된 리디렉션 URI 추가:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```

#### Kakao
1. [Kakao Developers](https://developers.kakao.com/) 접속
2. 애플리케이션 생성 후 **카카오 로그인 활성화**
3. Redirect URI 등록:
   ```
   http://localhost:8080/login/oauth2/code/kakao
   ```
4. **동의항목 설정**: 닉네임, 카카오계정(이메일) 활성화

#### Naver
1. [Naver Developers](https://developers.naver.com/) 접속
2. **오픈 API 이용 신청 > 네이버 로그인** 선택
3. Callback URL 등록:
   ```
   http://localhost:8080/login/oauth2/code/naver
   ```
4. **제공 정보 선택**: 이메일, 이름, 닉네임

---

### 환경 변수 설정

`src/main/resources/` 아래에 `application.yml`과 `application-secret.yml`을 구성하세요.

> ⚠️ `application-secret.yml`은 `.gitignore`에 반드시 추가해야 합니다.

**`application.yml`**
```yaml
spring:
  profiles:
    include: secret

  datasource:
    url: jdbc:mysql://localhost:3306/social_login?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  security:
    oauth2:
      client:
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response

jwt:
  expiration: 3600000          # Access Token 만료 (1시간, ms)
  refresh-expiration: 604800000 # Refresh Token 만료 (7일, ms)
```

**`application-secret.yml`**
```yaml
spring:
  datasource:
    username: YOUR_DB_USERNAME
    password: YOUR_DB_PASSWORD

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
            scope:
              - email
              - profile
          kakao:
            client-id: YOUR_KAKAO_REST_API_KEY
            client-secret: YOUR_KAKAO_CLIENT_SECRET
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
            authorization-grant-type: authorization_code
            client-authentication-method: client_secret_post
            scope:
              - profile_nickname
              - account_email
          naver:
            client-id: YOUR_NAVER_CLIENT_ID
            client-secret: YOUR_NAVER_CLIENT_SECRET
            redirect-uri: "{baseUrl}/login/oauth2/code/naver"
            authorization-grant-type: authorization_code
            scope:
              - name
              - email

jwt:
  secret: YOUR_JWT_SECRET_KEY   # 32자 이상 권장
```

**`.gitignore` 필수 추가 항목**
```
application-secret.yml
```

---

### 실행 방법

```bash
# 저장소 클론
git clone https://github.com/your-username/social-login-module.git
cd social-login-module

# MySQL DB 생성
mysql -u root -p
CREATE DATABASE social_login CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;

# 빌드
./gradlew clean build

# 실행
./gradlew bootRun
```

서버 기동 후 아래 URL로 접속 확인:
```
http://localhost:8080
http://localhost:8080/swagger-ui/index.html  # Swagger UI
```

---

## 📁 프로젝트 구조

```
src/
├── main/
│   ├── java/com/socialLogin/
│   │   │
│   │   ├── domain/                              # 📦 도메인 단위 비즈니스 로직
│   │   │   │
│   │   │   ├── user/                            # 유저 도메인
│   │   │   │   ├── controller/
│   │   │   │   │   └── UserController.java
│   │   │   │   ├── service/
│   │   │   │   │   └── UserService.java
│   │   │   │   ├── repository/
│   │   │   │   │   └── UserRepository.java
│   │   │   │   ├── entity/
│   │   │   │   │   └── User.java                # BaseEntity 상속
│   │   │   │   └── dto/
│   │   │   │       ├── UserResponse.java
│   │   │   │       └── OAuth2UserInfo.java      # 소셜 유저 정보 추상화
│   │   │   │
│   │   │   └── auth/                            # 인증 도메인
│   │   │       ├── controller/
│   │   │       │   └── AuthController.java
│   │   │       ├── service/
│   │   │       │   └── CustomOAuth2UserService.java
│   │   │       ├── handler/
│   │   │       │   ├── OAuth2SuccessHandler.java
│   │   │       │   └── OAuth2FailureHandler.java
│   │   │       └── dto/
│   │   │           ├── GoogleUserInfo.java
│   │   │           ├── KakaoUserInfo.java
│   │   │           └── NaverUserInfo.java
│   │   │
│   │   └── global/                              # 🌐 전역 공통 모듈
│   │       ├── config/
│   │       │   ├── SecurityConfig.java          # Spring Security 설정
│   │       │   └── SwaggerConfig.java           # Swagger / OpenAPI 설정
│   │       │
│   │       ├── common/
│   │       │   └── BaseEntity.java              # 공통 컬럼 (id, createdAt, updatedAt)
│   │       │
│   │       ├── jwt/
│   │       │   ├── JwtTokenProvider.java        # JWT 생성 / 검증
│   │       │   ├── JwtAuthenticationFilter.java # JWT 인증 필터
│   │       │   └── JwtProperties.java           # JWT 설정값 바인딩
│   │       │
│   │       └── exception/
│   │           ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │           ├── CustomException.java         # 커스텀 예외 베이스
│   │           └── ErrorCode.java               # 에러 코드 Enum
│   │
│   └── resources/
│       ├── application.yml
│       └── application-secret.yml               # ⚠️ Git 제외 대상
│
└── test/
    └── java/com/socialLogin/
        ├── domain/
        │   ├── auth/
        │   │   └── CustomOAuth2UserServiceTest.java
        │   └── user/
        │       └── UserServiceTest.java
        └── global/
            └── jwt/
                └── JwtTokenProviderTest.java
```

---

## 📡 API 명세

### 소셜 로그인 진입점

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/oauth2/authorize/google` | 구글 로그인 페이지로 리다이렉트 |
| GET | `/oauth2/authorize/kakao` | 카카오 로그인 페이지로 리다이렉트 |
| GET | `/oauth2/authorize/naver` | 네이버 로그인 페이지로 리다이렉트 |

### OAuth2 콜백 (Spring 자동 처리)

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/login/oauth2/code/google` | 구글 OAuth2 콜백 |
| GET | `/login/oauth2/code/kakao` | 카카오 OAuth2 콜백 |
| GET | `/login/oauth2/code/naver` | 네이버 OAuth2 콜백 |

### 인증 API

| Method | URL | 설명 | 인증 필요 |
|--------|-----|------|----------|
| POST | `/api/auth/refresh` | Access Token 재발급 | Refresh Token |
| POST | `/api/auth/logout` | 로그아웃 | ✅ |

### 사용자 API

| Method | URL | 설명 | 인증 필요 |
|--------|-----|------|----------|
| GET | `/api/users/me` | 현재 로그인 유저 정보 조회 | ✅ |

### 응답 예시

**GET `/api/users/me`**
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "홍길동",
  "provider": "kakao",
  "createdAt": "2025-03-20T12:00:00",
  "updatedAt": "2025-03-20T12:00:00"
}
```

**POST `/api/auth/refresh`**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

---

## 🔄 인증 플로우

```
Client                   Server (Spring Boot)        OAuth2 Provider
  │                             │                          │
  │  GET /oauth2/authorize/     │                          │
  │  {google|kakao|naver}       │                          │
  │────────────────────────────>│                          │
  │                             │   Redirect to Provider   │
  │<────────────────────────────│─────────────────────────>│
  │                             │                          │
  │         유저가 소셜 플랫폼에서 로그인                    │
  │<───────────────────────────────────────────────────────│
  │                             │                          │
  │  GET /login/oauth2/code/    │                          │
  │  {provider}?code=AUTH_CODE  │                          │
  │────────────────────────────>│                          │
  │                             │  Code → Access Token 교환 │
  │                             │─────────────────────────>│
  │                             │  UserInfo 응답            │
  │                             │<─────────────────────────│
  │                             │                          │
  │                             │  신규: DB 저장 (회원가입)  │
  │                             │  기존: DB 조회 후 업데이트 │
  │                             │──────────────┐            │
  │                             │<─────────────┘            │
  │                             │                           │
  │                             │  JWT 발급 (SuccessHandler) │
  │  Access Token + Refresh Token                           │
  │<────────────────────────────│                           │
  │                             │                           │
  │  API 요청 (Authorization: Bearer {accessToken})         │
  │────────────────────────────>│                           │
  │                             │  JwtAuthenticationFilter  │
  │                             │  토큰 검증 → SecurityContext│
  │  응답                        │──────────────┐            │
  │<────────────────────────────│<─────────────┘            │
```

---

## ⚙️ 주요 설정

### global/common/BaseEntity.java

> `id`, `createdAt`, `updatedAt` 등 공통 컬럼은 모든 엔티티가 BaseEntity를 상속받아 사용합니다.

```java
// global/common/BaseEntity.java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

> ⚠️ `@EnableJpaAuditing`은 `global/config/` 또는 메인 클래스에 반드시 추가해야 합니다.

```java
// SocialLoginApplication.java
@EnableJpaAuditing
@SpringBootApplication
public class SocialLoginApplication { ... }
```

---

### domain/user/entity/User.java

```java
// domain/user/entity/User.java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {  // BaseEntity에서 id, createdAt, updatedAt 상속

    private String email;
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Provider provider;  // GOOGLE, KAKAO, NAVER

    private String providerId;

    public static User of(OAuth2UserInfo userInfo) { ... }
}
```

---

### global/config/SecurityConfig.java

```java
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/oauth2/**", "/login/**",
                                 "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(e -> e.baseUri("/oauth2/authorize"))
                .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter,
                             UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

---

### global/jwt/JwtTokenProvider.java

```java
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(Long userId) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
            .signWith(getSigningKey())
            .compact();
    }

    public boolean validateToken(String token) { ... }

    public Long getUserId(String token) { ... }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(UTF_8));
    }
}
```

---

### global/exception/ErrorCode.java

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 플랫폼입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다.");

    private final HttpStatus status;
    private final String message;
}
```

---

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 테스트 리포트 확인 (macOS 기준)
open build/reports/tests/test/index.html
```

> 본 프로젝트는 MySQL을 메인 DB로 사용합니다.  
> 테스트 시에는 `@DataJpaTest` 슬라이스 테스트 또는 Testcontainers를 활용한 MySQL 컨테이너 사용을 권장합니다.

**build.gradle 테스트 의존성**
```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
testImplementation 'org.springframework.boot:spring-boot-starter-security-oauth2-client-test'
testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

---

## 🔧 트러블슈팅

### Q. 카카오 로그인 시 이메일이 null로 들어와요
카카오는 이메일 제공이 **선택 동의** 항목입니다.  
Kakao Developers 콘솔에서 **동의항목 > 카카오계정(이메일)** 을 **필수 동의**로 변경하거나,  
이메일 없이도 동작하도록 `providerId` 기반 식별 로직을 별도로 구현하세요.

### Q. Redirect URI mismatch 오류가 발생해요
각 플랫폼 개발자 콘솔에 등록된 Redirect URI와 `application.yml`의 URI가 **정확히 일치**하는지 확인하세요.  
포트 번호와 경로 끝의 슬래시 여부까지 동일해야 합니다.

### Q. MySQL 연결이 안 돼요
- `application-secret.yml`의 DB 접속 정보를 확인하세요.
- MySQL 서버 실행 여부를 확인하세요: `mysql.server status`
- 해당 DB 유저에 접근 권한이 있는지 확인하세요.

### Q. 네이버 로그인 후 유저 정보 파싱이 안 돼요
네이버 UserInfo 응답은 `response` 키 안에 실제 데이터가 **중첩**되는 구조입니다.  
`application.yml`의 `user-name-attribute: response` 설정과 함께  
`NaverUserInfo`에서 한 단계 더 파싱해야 합니다.

```java
// domain/auth/dto/NaverUserInfo.java
@SuppressWarnings("unchecked")
public NaverUserInfo(Map<String, Object> attributes) {
    // 네이버는 response 키 안에 유저 정보가 감싸져 있음
    this.attributes = (Map<String, Object>) attributes.get("response");
}
```

### Q. JPA Auditing이 동작하지 않아요 (`createdAt`, `updatedAt`이 null)
`@EnableJpaAuditing`이 누락된 경우입니다.  
메인 클래스 또는 `global/config/` 아래 별도 Config 클래스에 어노테이션을 추가하세요.

```java
@EnableJpaAuditing
@SpringBootApplication
public class SocialLoginApplication { ... }
```

---

## 🤝 기여 방법

1. 이 저장소를 Fork합니다.
2. 새 브랜치를 생성합니다. (`git checkout -b feature/apple-login`)
3. 변경사항을 커밋합니다. (`git commit -m 'feat: Apple 로그인 추가'`)
4. 브랜치에 Push합니다. (`git push origin feature/apple-login`)
5. Pull Request를 생성합니다.

---

## 📄 라이선스

이 프로젝트는 [MIT License](./LICENSE) 하에 배포됩니다.

---

<div align="center">
  Made with ❤️ using Spring Boot 4 & OAuth2
</div>