package com.socialogin.module.global.oauth.userinfo;

import java.util.Map;

/**
 * provider 응답 Map을 안전하게 읽기 위한 작은 유틸리티입니다.
 *
 * <p>역할:
 * - 각 UserInfo 구현체가 Map 캐스팅 코드를 반복하지 않게 합니다.
 * - provider별 JSON 구조가 달라도 null-safe하게 값을 꺼내도록 돕습니다.
 *
 * <p>메모리 관점:
 * - UserInfo 객체는 필요한 문자열만 보관하고 원본 attributes Map을 오래 들고 있지 않습니다.
 */
final class OAuthAttributes {
    private OAuthAttributes() {
        // 유틸리티 클래스는 객체를 만들 필요가 없으므로 private 생성자로 생성을 막습니다.
    }

    /**
     * 응답 Map에서 문자열 값을 꺼냅니다. 숫자 id도 문자열로 변환합니다.
     */
    static String stringValue(Map<String, Object> attributes, String key) {
        // provider 응답 Map에서 key에 해당하는 원본 값을 꺼냅니다.
        Object value = attributes.get(key);

        // 값이 없으면 null을 반환하고, 숫자 id 같은 값은 String.valueOf로 문자열로 통일합니다.
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 중첩 JSON 객체를 Map으로 안전하게 변환합니다.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> mapValue(Map<String, Object> attributes, String key) {
        // 중첩 JSON 객체는 Jackson 역직렬화 후 Map 형태로 들어옵니다.
        Object value = attributes.get(key);

        // Java 21 pattern matching으로 "Map이면 map 변수로 받기"를 수행합니다.
        if (value instanceof Map<?, ?> map) {
            // 실제 provider 응답 구조를 신뢰하고 Map<String, Object>로 캐스팅합니다.
            return (Map<String, Object>) map;
        }

        // 중첩 객체가 없으면 null 대신 빈 Map을 반환해 호출부 null 체크를 줄입니다.
        return Map.of();
    }
}
