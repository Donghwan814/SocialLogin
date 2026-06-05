package com.socialogin.module.global.exception;

import com.socialogin.module.global.rsdata.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 애플리케이션 전역 예외를 공통 응답(RsData)으로 변환하는 클래스입니다.
 *
 * <p>역할:
 * - Controller에서 try-catch를 반복하지 않도록 예외 응답 생성을 한 곳에 모읍니다.
 * - 예외 타입별로 HTTP 상태 코드와 에러 메시지를 일관되게 내려줍니다.
 * - 성공/실패 모두 RsData 구조로 통일합니다(success=false, error={code,message}).
 *
 * <p>provider가 추가될 때:
 * - 보통 이 클래스는 수정하지 않습니다.
 * - 새 provider에서 발생하는 실패도 OAuthLoginException으로 감싸면 같은 포맷으로 응답됩니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 잘못된 provider 문자열을 요청한 경우의 응답을 만듭니다.
     */
    @ExceptionHandler(InvalidOAuthProviderException.class)
    public ResponseEntity<RsData<Void>> handleInvalidOAuthProvider(InvalidOAuthProviderException exception) {
        // 예외가 들고 있는 표준 ErrorCode를 꺼냅니다.
        ErrorCode errorCode = exception.getErrorCode();

        // ErrorCode의 HTTP status와 공통 실패 body를 조합해 응답합니다.
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(RsData.fail(errorCode, exception.getMessage()));
    }

    /**
     * 소셜 로그인 과정의 도메인 실패를 공통 응답으로 변환합니다.
     */
    @ExceptionHandler(OAuthLoginException.class)
    public ResponseEntity<RsData<Void>> handleOAuthLogin(OAuthLoginException exception) {
        // OAuthService/OAuthClient가 던진 도메인 예외의 ErrorCode를 꺼냅니다.
        ErrorCode errorCode = exception.getErrorCode();

        // provider 연동 실패, 이메일 미제공 등 의미 있는 오류를 공통 포맷으로 내려줍니다.
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(RsData.fail(errorCode, exception.getMessage()));
    }

    /**
     * callback 요청에서 code 같은 필수 query parameter가 빠진 경우를 처리합니다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RsData<Void>> handleMissingParameter(MissingServletRequestParameterException exception) {
        // 현재 callback에서 필수 query parameter는 code이므로 code 누락 에러로 통일합니다.
        ErrorCode errorCode = ErrorCode.MISSING_AUTHORIZATION_CODE;

        // query parameter 누락도 공통 실패 응답으로 맞춥니다.
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(RsData.fail(errorCode));
    }

    /**
     * 예상하지 못한 서버 오류를 외부에 그대로 노출하지 않고 표준 메시지로 감쌉니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleException(Exception exception) {
        // 예상하지 못한 예외는 내부 구현 상세를 숨기기 위해 일반 서버 에러로 처리합니다.
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        // 실제 exception 메시지를 외부에 노출하지 않고 표준 메시지만 내려줍니다.
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(RsData.fail(errorCode));
    }
}
