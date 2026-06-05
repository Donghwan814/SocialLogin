package com.socialogin.module.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 실패 응답에 사용할 표준 에러 목록입니다.
 */
@Getter
public enum ErrorCode {
    // 요청 값이 잘못된 경우입니다.
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),
    MISSING_AUTHORIZATION_CODE(HttpStatus.BAD_REQUEST, "MISSING_AUTHORIZATION_CODE", "소셜 로그인 인증 코드가 누락되었습니다."),
    INVALID_AUTHORIZATION_CODE(HttpStatus.BAD_REQUEST, "INVALID_AUTHORIZATION_CODE", "소셜 로그인 인증 코드가 올바르지 않습니다."),
    INVALID_OAUTH2_STATE(HttpStatus.BAD_REQUEST, "INVALID_OAUTH2_STATE", "OAuth2 state 값이 올바르지 않습니다."),
    INVALID_REDIRECT_URI(HttpStatus.BAD_REQUEST, "INVALID_REDIRECT_URI", "소셜 로그인 리다이렉트 URI가 올바르지 않습니다."),

    // provider 이름이 잘못된 경우입니다.
    INVALID_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "INVALID_SOCIAL_PROVIDER", "소셜 로그인 제공자 정보가 올바르지 않습니다."),
    UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "UNSUPPORTED_SOCIAL_PROVIDER", "지원하지 않는 소셜 로그인 제공자입니다."),

    // 인증/인가에 실패한 경우입니다.
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "토큰 형식이 잘못되었거나 서명이 유효하지 않습니다."),
    SOCIAL_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "SOCIAL_AUTHENTICATION_FAILED", "소셜 로그인 인증에 실패했습니다."),
    SOCIAL_ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "SOCIAL_ACCESS_TOKEN_EXPIRED", "소셜 로그인 Access Token이 만료되었습니다."),
    SOCIAL_ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "SOCIAL_ACCESS_TOKEN_INVALID", "소셜 로그인 Access Token이 유효하지 않습니다."),
    SOCIAL_ID_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "SOCIAL_ID_TOKEN_INVALID", "소셜 로그인 ID Token이 유효하지 않습니다."),
    SOCIAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SOCIAL_ACCESS_DENIED", "소셜 계정 접근 권한이 거부되었습니다."),

    // 소셜 계정 정보가 부족하거나 충돌한 경우입니다.
    SOCIAL_EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "SOCIAL_EMAIL_NOT_PROVIDED", "소셜 계정에서 이메일 정보를 제공하지 않았습니다."),
    SOCIAL_PROFILE_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "SOCIAL_PROFILE_NOT_PROVIDED", "소셜 계정 프로필 정보를 가져올 수 없습니다."),
    SOCIAL_PROFILE_MAPPING_FAILED(HttpStatus.BAD_REQUEST, "SOCIAL_PROFILE_MAPPING_FAILED", "소셜 계정 프로필 정보를 처리할 수 없습니다."),
    SOCIAL_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "SOCIAL_MEMBER_NOT_FOUND", "소셜 로그인으로 가입된 회원을 찾을 수 없습니다."),
    SOCIAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "SOCIAL_ACCOUNT_NOT_FOUND", "연동된 소셜 계정을 찾을 수 없습니다."),
    SOCIAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "SOCIAL_ACCOUNT_ALREADY_LINKED", "이미 다른 회원에게 연동된 소셜 계정입니다."),
    DUPLICATED_EMAIL_WITH_DIFFERENT_PROVIDER(HttpStatus.CONFLICT, "DUPLICATED_EMAIL_WITH_DIFFERENT_PROVIDER", "이미 다른 로그인 방식으로 가입된 이메일입니다."),
    EMAIL_ALREADY_LINKED_WITH_OTHER_PROVIDER(HttpStatus.CONFLICT, "EMAIL_ALREADY_LINKED_WITH_OTHER_PROVIDER", "이미 다른 소셜 로그인으로 가입된 이메일입니다."),

    // provider 외부 API 호출에 실패한 경우입니다.
    SOCIAL_PROVIDER_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "SOCIAL_PROVIDER_NOT_CONFIGURED", "소셜 로그인 제공자 설정이 올바르지 않습니다."),
    SOCIAL_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, "SOCIAL_TOKEN_EXCHANGE_FAILED", "소셜 로그인 토큰 교환에 실패했습니다."),
    SOCIAL_USER_INFO_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "SOCIAL_USER_INFO_REQUEST_FAILED", "소셜 계정 사용자 정보 요청에 실패했습니다."),
    SOCIAL_PROVIDER_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "SOCIAL_PROVIDER_RESPONSE_INVALID", "소셜 로그인 제공자 응답이 올바르지 않습니다."),
    SOCIAL_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SOCIAL_PROVIDER_UNAVAILABLE", "소셜 로그인 제공자를 일시적으로 사용할 수 없습니다."),
    SOCIAL_PROVIDER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "SOCIAL_PROVIDER_TIMEOUT", "소셜 로그인 제공자 응답 시간이 초과되었습니다."),

    // 서버 내부 처리에 실패한 경우입니다.
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    SOCIAL_LOGIN_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SOCIAL_LOGIN_PROCESSING_FAILED", "소셜 로그인 처리 중 오류가 발생했습니다."),
    SOCIAL_ACCOUNT_LINK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SOCIAL_ACCOUNT_LINK_FAILED", "소셜 계정 연동 중 오류가 발생했습니다."),
    SOCIAL_TOKEN_ISSUE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SOCIAL_TOKEN_ISSUE_FAILED", "로그인 토큰 발급 중 오류가 발생했습니다.");

    // HTTP 응답 상태입니다.
    private final HttpStatus httpStatus;

    // 프론트엔드가 분기하기 쉬운 문자열 코드입니다.
    private final String code;

    // 기본 에러 메시지입니다.
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
