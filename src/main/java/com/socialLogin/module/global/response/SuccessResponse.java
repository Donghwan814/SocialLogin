package com.socialLogin.module.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessResponse {

    OK(HttpStatus.OK, "요청 성공!", OK), // 200
    CREATED(HttpStatus.CREATED, "생성 완료!", CREATED); // 201

    // 외부에서 값을 변경하지 못하게 막고 공통된 응답을 제공하기 위해서 사용함
    private final HttpStatus status;
    private final String message;
    private final String code;
}
