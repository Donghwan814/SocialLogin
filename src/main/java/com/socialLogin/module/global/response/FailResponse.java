package com.socialLogin.module.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FailResponse {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청", "BAD_REQUEST"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 필요", "UNAUTHORIZED"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스가 없습니다.", "NOT_FOUND"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않는 메서드입니다.", "METHOD_NOT_ALLOWED"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류", "INTERNAL_SERVER_ERROR");

    private final HttpStatus status;
    private final String message;
    private final String code;
}
