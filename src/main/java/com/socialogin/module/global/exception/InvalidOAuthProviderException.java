package com.socialogin.module.global.exception;

import lombok.Getter;

/**
 * 지원하지 않는 provider 문자열이 들어왔을 때 던지는 예외입니다.
 */
@Getter
public class InvalidOAuthProviderException extends RuntimeException {
    // 잘못된 provider 요청은 400 계열 에러로 응답합니다.
    private final ErrorCode errorCode;

    public InvalidOAuthProviderException(String provider) {
        super("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        this.errorCode = ErrorCode.INVALID_SOCIAL_PROVIDER;
    }
}
