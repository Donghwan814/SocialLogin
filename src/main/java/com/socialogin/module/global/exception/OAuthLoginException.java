package com.socialogin.module.global.exception;

import lombok.Getter;

/**
 * 소셜 로그인 중 발생한 실패를 표현하는 예외입니다.
 */
@Getter
public class OAuthLoginException extends RuntimeException {
    // GlobalExceptionHandler가 응답 상태와 에러 코드를 만들 때 사용합니다.
    private final ErrorCode errorCode;

    public OAuthLoginException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public OAuthLoginException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OAuthLoginException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
