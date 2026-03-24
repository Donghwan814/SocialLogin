package com.socialLogin.module.global.globalexception;

import com.socialLogin.module.global.exception.ServiceException;
import com.socialLogin.module.global.rsData.RsData;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice // @ControllerAdvice + @ResponseBody의 의미를 함께 가지며, 예외 응답을 JSON 형태로 반환할 때 사용
/**
 * 애플리케이션 전역에서 발생하는 예외를 한 곳에서 처리하는 클래스
 * 모든 컨트롤러에서 발생한 예외를 공통 형식(RsData)으로 변환하여 클라이언트에 반환한다.
 */
public class GlobalExceptionHandler {

    /**
     * 존재하지 않는 데이터를 조회하려고 할 때 처리
     * 예: DB에 없는 ID로 조회하는 경우
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handle(NoSuchElementException exception) {
        return new ResponseEntity<>(
                new RsData<>("404-1", "해당 데이터가 존재하지 않습니다."),
                NOT_FOUND
        );
    }

    /**
     * @PathVariable, @RequestParam 등의 유효성 검사 실패 처리
     * 예: /news/0, /applicants?page=0
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RsData<Void>> handle(ConstraintViolationException exception) {

        String message = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + " : " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("\n"));

        return new ResponseEntity<>(
                new RsData<>("400-1", message),
                BAD_REQUEST
        );
    }

    /**
     * @Valid가 붙은 RequestBody DTO의 유효성 검사 실패 처리
     * 예: {"name":""}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handle(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + " : " + fieldError.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");

        return new ResponseEntity<>(new RsData<>("400-2", message), BAD_REQUEST);
    }

    /**
     * JSON 요청 본문 자체가 잘못된 경우 처리
     * 예: JSON 문법 오류, 타입 불일치
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<Void>> handle(HttpMessageNotReadableException exception) {
        return new ResponseEntity<>(
                new RsData<>("400-3", "요청 본문 형식이 올바르지 않습니다."),
                BAD_REQUEST
        );
    }

    /**
     * 요청 파라미터의 타입이 맞지 않을 때 처리
     * 예: id는 Long이어야 하는데 문자열이 들어온 경우
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RsData<Void>> handle(MethodArgumentTypeMismatchException exception) {
        return new ResponseEntity<>(
                new RsData<>("400-4", exception.getName() + " 값의 형식이 올바르지 않습니다."),
                BAD_REQUEST
        );
    }

    /**
     * 인증이 필요한 요청에 대해 인증이 되지 않은 경우 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RsData<Void>> handle(AuthenticationException exception) {
        return new ResponseEntity<>(
                new RsData<>("401-1", "인증이 필요합니다."),
                UNAUTHORIZED
        );
    }

    /**
     * Service 계층에서 발생한 비즈니스 예외 처리
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> handle(ServiceException exception) {
        return new ResponseEntity<>(exception.getRsData(), BAD_REQUEST);
    }

    /**
     * 지원하지 않는 HTTP 메서드로 요청한 경우 처리
     * 예: GET만 허용하는 API에 POST 요청
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RsData<Void>> handle(HttpRequestMethodNotSupportedException exception) {
        return new ResponseEntity<>(
                new RsData<>("405-1", "지원하지 않는 요청 방식입니다."),
                METHOD_NOT_ALLOWED
        );
    }

    /**
     * 그 외 처리되지 않은 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handle(Exception exception) {
        exception.printStackTrace();
        return new ResponseEntity<>(
                new RsData<>("500-1", "서버 내부 오류가 발생했습니다."),
                INTERNAL_SERVER_ERROR
        );
    }
}