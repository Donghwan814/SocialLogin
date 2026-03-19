package com.socialLogin.module.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RsData<T> (

    String resultCode,
    @JsonIgnore
    int statusCode,
    String message,
    T data
) {
    public RsData(String resultCode, String message) {
        this(resultCode, message, null);
    }

    public RsData(String resultCode, String message, T data) {
        this(resultCode, Integer.parseInt(resultCode.split("-", 2)[0]), message, data);
    }
}