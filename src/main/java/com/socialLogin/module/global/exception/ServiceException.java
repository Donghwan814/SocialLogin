package com.socialLogin.module.global.exception;

// 비즈니스 로직 Service 레벨에서 직접 던지는 커스텀 예외 정의

import com.socialLogin.module.global.rsData.RsData;

/**
 * controller는 요청을 받고 그에 대한 응답 호출 관리하는 기능
 * Service에서 해당 예외를 throw하면, 보통 전역 예외 처리기(@RestControllerAdvice)에서
 * 이를 잡아 RsData 형식의 일관된 에러 응답으로 변환하여 클라이언트에 반환한다.
 *
 * 사용 이유 : Service 로직에서 if-else로 직접 응답을 반환하는 대신 예외를 던져 코드 흐름을 단순화할 수 있다
 */

// 체크 예외: 컴파일러가 반드시 처리하라고 강제하는 예외(try catch 또는 throw로 던지며 대표적인 예외는 IOException(입출력 예외), 언체크 : 강제하지 않는 예외 대표적 ex) RunTimeException, NPE, IllegalArgumentException 등
public class ServiceException extends RuntimeException { // RunTimeException의 경우 언체크 예외라고 부름 : 서비스 비즈니스 예외 복구를 위해 즉시 잡아야 하는 예외보다는 사용자에게 에러 응답을 주면 됨
    // ServiceException 객체가 가지고 있을 정보
    private final String resultCode;
    private final String message;

    public ServiceException(String resultCode, String message) { // 매개변수 있는 생성자 생성(예외 객체 만들기 위한 코드)
        super(resultCode + " : " + message); // 부모 클래스 호출 ex) resultCode (404-1) : message("해당 게시글이 존재하지 않습니다.")
        this.resultCode = resultCode;
        this.message = message;
    }

    public RsData<Void> getRsData() { // 예외 객체가 가지고 있는 정보를 이용해서 RsData 응답 객체로 바꾸기 위한 메서드
        return new RsData<>(resultCode, message, null);
    }
}
