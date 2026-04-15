# 🔐 Spring Boot Social Login Module

> Spring Boot 기반 Google · Kakao · Naver OAuth2 소셜 로그인 통합 모듈

---

## 📖 프로젝트 소개

이 프로젝트는 **Spring Boot + Spring Security OAuth2 Client**를 기반으로  
**Google, Kakao, Naver 소셜 로그인**을 통합 구현하는 **재사용 가능한 인증 모듈**입니다.

단순 로그인 기능만 구현하는 것이 아니라,  
소셜 로그인 이후 사용자 정보를 조회하고, 회원가입 또는 기존 회원 매핑을 처리하며,  
향후 **JWT 인증 방식**까지 확장할 수 있도록 설계하는 것을 목표로 합니다.

---

## 🎯 개발 목표

- Google / Kakao / Naver 소셜 로그인 통합
- Provider별 사용자 정보 응답 구조 표준화
- 최초 로그인 시 자동 회원가입 처리
- 기존 회원 로그인 연동
- 확장 가능한 모듈형 인증 구조 설계
- JWT 기반 인증 시스템으로 확장 가능하도록 구조화

---

## ✨ 주요 기능

### 1. 소셜 로그인 지원
- Google OAuth2 로그인
- Kakao OAuth2 로그인
- Naver OAuth2 로그인

### 2. 사용자 정보 통합 처리
- Provider마다 다른 응답 형식을 공통 인터페이스로 추상화
- 이메일, 닉네임, Provider ID 등 핵심 정보 통합 관리

### 3. 회원가입 / 로그인 자동 처리
- 최초 로그인 시 회원가입 자동 수행
- 기존 회원이면 로그인 처리
- 소셜 Provider와 Provider별 고유 ID를 기준으로 사용자 식별

### 4. 확장 가능한 인증 구조
- JWT 발급 로직 추가 가능
- Refresh Token 저장 구조 확장 가능
- Redis, Role 기반 권한 관리 등으로 확장 가능

---

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot |
| Security | Spring Security |
| OAuth | Spring Security OAuth2 Client |
| ORM | Spring Data JPA |
| Database | MySQL |
| Build Tool | Gradle |
| Docs | Swagger / SpringDoc OpenAPI |
| Utility | Lombok |
| Optional | JWT, Redis |

---

## 📁 프로젝트 구조

```text
src/
├── main/
│   ├── java/com/example/sociallogin/
│   │   ├── domain/
│   │   │   ├── auth/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── handler/
│   │   │   │   └── service/
│   │   │   └── user/
│   │   │       ├── entity/
│   │   │       ├── repository/
│   │   │       └── service/
│   │   └── global/
│   │       ├── config/
│   │       ├── exception/
│   │       ├── oauth/
│   │       └── jwt/
│   └── resources/
│       ├── application.yml
│       └── application-secret.yml
└── test/