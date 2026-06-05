package com.socialogin.module.domain.user.entity;

/**
 * 서비스 내부 사용자 권한입니다.
 *
 * <p>역할:
 * - JWT role claim과 Spring Security 권한 생성의 기준이 됩니다.
 * - 권한 문자열을 enum으로 관리해 오타를 줄입니다.
 */
public enum UserRole {
    // 일반 로그인 사용자를 의미합니다.
    USER,

    // 관리자 권한을 의미합니다. 추후 관리자 API 보호에 사용할 수 있습니다.
    ADMIN;

    /**
     * Spring Security가 기대하는 ROLE_ 접두사 권한명으로 변환합니다.
     */
    public String getAuthority() {
        // Spring Security SimpleGrantedAuthority가 기대하는 ROLE_ 접두사 형식으로 변환합니다.
        return "ROLE_" + name();
    }
}
