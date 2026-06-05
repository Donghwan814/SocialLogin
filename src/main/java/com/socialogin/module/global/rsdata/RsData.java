package com.socialogin.module.global.rsdata;

import com.socialogin.module.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * API 공통 응답 래퍼입니다.
 */
@Schema(description = "API 공통 응답")
public record RsData<T>(
        // true면 성공 응답, false면 실패 응답입니다.
        @Schema(description = "요청 성공 여부", example = "true")
        boolean success,

        // 성공했을 때 실제 응답 DTO가 들어갑니다.
        @Schema(description = "성공 응답 데이터")
        T data,

        // 실패했을 때 에러 코드/메시지가 들어갑니다.
        @Schema(description = "실패 시 에러 정보")
        ErrorInfo error,

        // 응답이 만들어진 서버 시각입니다.
        @Schema(description = "응답 생성 시각", example = "2026-05-20T12:00:00")
        LocalDateTime timestamp
) {
    @Schema(description = "API 에러 정보")
    public record ErrorInfo(
            // 프론트엔드가 분기 처리하기 좋은 안정적인 문자열 코드입니다.
            @Schema(description = "에러 코드", example = "INVALID_SOCIAL_PROVIDER")
            String code,

            // 사용자 또는 개발자에게 보여줄 메시지입니다.
            @Schema(description = "에러 메시지", example = "지원하지 않는 소셜 로그인 제공자입니다.")
            String message
    ) {
    }

    public static <T> RsData<T> success(T data) {
        // 성공 응답은 success=true, data=응답값, error=null로 통일합니다.
        return new RsData<>(true, data, null, LocalDateTime.now());
    }

    public static <T> RsData<T> fail(ErrorCode errorCode) {
        // 기본 에러 메시지를 사용하는 실패 응답입니다.
        return new RsData<>(false, null, new ErrorInfo(errorCode.getCode(), errorCode.getMessage()), LocalDateTime.now());
    }

    public static <T> RsData<T> fail(ErrorCode errorCode, String message) {
        // 상황별 상세 메시지를 덮어쓸 수 있는 실패 응답입니다.
        return new RsData<>(false, null, new ErrorInfo(errorCode.getCode(), message), LocalDateTime.now());
    }
}
