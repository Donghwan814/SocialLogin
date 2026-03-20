package com.socialLogin.module.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RsData<T> (

    String resultCode, // ex) 404-1 개발할 때 404 안에서도 뭐 때문에 404 뜨는지 다르기 때문에 세부 코드 적어서 확인 가능하도록 설정
    @JsonIgnore
    int statusCode, // ex) 404 보여질 때는 한눈에 알아보기 쉽게
    String message, // ex) 파일을 찾을 수 없습니다. 즉, 어떤 오류 메시지인지 나타낼 수 있도록
    T data // ex) int형 또는 String형 즉, 오류가 일어난 값들을 나타내며 요청값들이 다 다르기에 제너릭 타입 T를 사용
) {
    public RsData(String resultCode, String message) {
        this(resultCode, message, null);
    }

    public RsData(String resultCode, String message, T data) {
        this(resultCode, Integer.parseInt(resultCode.split("-", 2)[0]), message, data);
    }
}