package com.socialLogin.module.global.globalexception;

import com.socialLogin.module.global.rsData.RsData;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice // @ControllerAdvice(모든 컨트롤러 예외를 가로채 처리하는 클래스임을 명시) + @ResponseEntity.JSON 형태 응답을 자동으로 처리할 때 사용

/**
 * GlobalExceptionHandler : 전체 앱에서 발생하는 오류를 한 곳에서 잡아서 정리해줌
 * @RestControllerAdvice는 Controller 전체에서 발생하는 오류를 처리할거라고 선언 (어디서든 오류 터지면 이 클래스가 받아서 처리)
 */
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class) // 존재하지 않는 데이터를 찾으려고 할 때 ex) DB에 없는 ID로 조회하려고 할 때
    // ResponseEntity 사용을 해야 HTTP 응답 상태코드 지정이 가능함 (상태코드 + 헤더 바디 한 번에 담아서 보내는 역할) + 헤더는 Spring이 자동 계산해줌 (JWT Authorization Bearer 사용일 땐 직접 정의)
    // void 사용 이유 : 오류 응답에 data를 담을 이유가 없기 때문에 아무것도 안들어오는 void 값 사용
    public ResponseEntity<RsData<Void>> handle(NoSuchElementException exception) {
        return new ResponseEntity<>(new RsData<>("404-1", "해당 데이터가 존재하지 않습니다."), NOT_FOUND); // resultCode, message 바디값 + 상태 코드인 NOT_FOUND(404) 담음)
    }

    /**
     * @PathVariable, @RequestParam 유효성 검사 실패 처리
     * 예) /news/0, /applicants?page=0
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public org.springframework.http.ResponseEntity<RsData<Void>> handle(ConstraintViolationException exception) {

        String message = exception.getConstraintViolations() // = Set<ConstraintViolation<?>> 이렇게 들어있음
                .stream()
                .map(violation -> violation.getPropertyPath() + " : " + violation.getMessage()) // 오류가 발생한 위치 오류 메시지 꺼내서 문자열 하나로 만듦 ex) getNews.id : 1 이상이어야 합니다.
                .sorted() // Set은 순서 보장이 안되기 때문에 알파벳 순서대로 정렬
                .collect(Collectors.joining("\n")); // 오류 메시지 줄바꿈("\n") 기준으로 하나의 문자열로 합쳐서 보여줌

        return new org.springframework.http.ResponseEntity<>(
                new RsData<>("400-1", message),
                BAD_REQUEST
        );
    }

    /**
     * @Valid 붙은 RequestBody JSON 필드 유효성 검사 실패할 때 ex) {"name":""} 필드가 비어져 있을 경우
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handle(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult() // DTO 바인딩과 검증 결과 담긴 객체를 가져옴
                .getFieldErrors() // BindingResult 중에서 검증 실패한 목록들을 가져옴
                .stream()
                .map(fieldError -> fieldError.getField() + " : " + fieldError.getDefaultMessage()) // 필드명 + 메시지 형태 문자열 하나로 변환 됨 getDefaultMessage() 메서드 통해서 필드에 설정한 검증 메시지 가져옴 (사용자는 메시지만 보여짐) 즉, 어떤 필드에서 왜 틀렸는지 보기 쉽게 문자열로 만드는 과정
                .findFirst() // 여러 에러들 중 첫 번째 에러 하나만 가져옴 (응답 메시지 길게 만들지 않기 위함 + 하나씩 수정하게 만들기 위해)
                .orElse("입력값이 올바르지 않습니다."); // 에러 메시지 없을 경우 사용

        return new ResponseEntity<>(new RsData<>("400-2", message), BAD_REQUEST);
    }
    /**
     * JSON 요청 본문 자체 잘못된 경우
     * ex) 문법 오류 및 타입 오류
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<Void>> handle(HttpMessageNotReadableException exception) {
        return new ResponseEntity<>(new RsData<>("400-3", "요청 본문 형식이 올바르지 않습니다."), BAD_REQUEST);
    }

    /**
     * 요청 파라미터 타입이 맞지 않을 때 처리
     * ex) id가 Long 값이어야 하는데 문자열로 들어온 경우
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RsData<Void>> handle(MethodArgumentTypeMismatchException exception) {
        return new ResponseEntity<>(new RsData<>("400-4",exception.getName() + "값 형식이 올바르지 않습니다."), BAD_REQUEST); // ex) id 값의 형식이 올바르지 않습니다.
    }



    /**
     * 관리자 로그인이 되어 있지 않은 사용자는 뉴스, 게시글, 지원자 조회, 인트로 등 CRUD 하지 못함 (인증 필요)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RsData<Void>> handle(Authentication exception) {
        return new ResponseEntity<>(new RsData<>("401-1", "인증이 필요합니다."), UNAUTHORIZED);
    }

    /**
     *  지원하지 않는 HTTP 메서드 요청 처리
     *  ex) GET 요청 보내야하는데 POST로 요청 보낼 때
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RsData<Void>> handle(HttpRequestMethodNotSupportedException exception) {
        return new ResponseEntity<>(new RsData<>("405-1", "지원하지 않는 요청 방식입니다."), METHOD_NOT_ALLOWED);
    }

    // 그 외 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handle(Exception exception) {
        exception.printStackTrace(); // ← 추가! 콘솔에서 원인 확인
        return new ResponseEntity<>(new RsData<>("500-1", "서버 내부 오류가 발생했습니다."), INTERNAL_SERVER_ERROR);
    }