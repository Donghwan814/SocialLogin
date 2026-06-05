package com.socialogin.module.global.oauth;

import com.socialogin.module.global.exception.InvalidOAuthProviderException;

import java.util.Arrays;

/**
 * 지원하는 소셜 로그인 제공자를 표현하는 enum입니다.
 *
 * <p>역할:
 * - URL path로 들어오는 "google", "kakao" 같은 문자열을 타입 안전한 값으로 변환합니다.
 * - provider 문자열 비교를 애플리케이션 곳곳에서 반복하지 않게 합니다.
 *
 * <p>provider가 추가될 때:
 * - 이 enum에 새 값을 추가합니다.
 * - OAuthClient 구현체와 application.yml 설정만 추가하면 나머지 흐름은 그대로 동작합니다.
 */
public enum OAuthProvider {
    // 이메일/비밀번호로 가입한 로컬 계정입니다. OAuth API 호출 대상은 아닙니다.
    LOCAL("local"),

    // Google OAuth/OpenID Connect provider입니다.
    GOOGLE("google"),

    // Kakao Login provider입니다.
    KAKAO("kakao"),

    // Naver Login provider입니다.
    NAVER("naver"),

    // Facebook Login provider입니다.
    FACEBOOK("facebook"),

    // GitHub OAuth App provider입니다.
    GITHUB("github");

    // URL path와 application.yml key에 공통으로 쓰는 provider 문자열입니다.
    private final String registrationId;

    OAuthProvider(String registrationId) {
        // enum 상수마다 provider 문자열을 저장합니다.
        this.registrationId = registrationId;
    }

    /**
     * 클라이언트가 path variable로 보내는 provider 문자열입니다.
     */
    public String getRegistrationId() {
        // 외부 요청/설정에서 쓰는 소문자 provider 이름을 반환합니다.
        return registrationId;
    }

    /**
     * 문자열 provider 값을 enum으로 변환합니다.
     *
     * <p>이 메서드에 변환 책임을 모아두면 Controller와 Service가 문자열 비교 로직을 갖지 않아도 됩니다.
     */
    public static OAuthProvider from(String provider) {
        // enum 전체를 순회하면서 요청 문자열과 registrationId가 일치하는 값을 찾습니다.
        return Arrays.stream(values())
                // 대소문자 차이는 허용해 google, GOOGLE 모두 처리합니다.
                .filter(value -> value.registrationId.equalsIgnoreCase(provider))
                // 일치하는 provider가 있으면 Optional에 담습니다.
                .findFirst()
                // 없으면 잘못된 provider 요청이므로 커스텀 예외를 던집니다.
                .orElseThrow(() -> new InvalidOAuthProviderException(provider));
    }

    /**
     * OAuth 로그인 API에서 사용할 수 있는 provider 문자열만 enum으로 변환합니다.
     *
     * <p>LOCAL은 User 테이블에는 저장되지만 외부 OAuth provider가 아니므로 authorize/login 흐름에서는 제외합니다.
     */
    public static OAuthProvider fromOAuthProvider(String provider) {
        // 먼저 문자열을 전체 provider enum으로 변환합니다.
        OAuthProvider resolvedProvider = from(provider);

        // LOCAL은 OAuthClient가 없기 때문에 OAuth API 요청으로 들어오면 잘못된 provider로 처리합니다.
        if (!resolvedProvider.isOAuthProvider()) {
            throw new InvalidOAuthProviderException(provider);
        }

        // Google/Kakao/Naver/Facebook/GitHub 같은 실제 OAuth provider만 반환합니다.
        return resolvedProvider;
    }

    /**
     * 외부 OAuth provider인지 확인합니다.
     */
    public boolean isOAuthProvider() {
        // LOCAL은 이메일/비밀번호 계정이므로 provider token 교환 대상이 아닙니다.
        return this != LOCAL;
    }

    /**
     * 사용자에게 안내할 provider 표시 이름입니다.
     */
    public String getDisplayName() {
        // 예외 메시지에서 GOOGLE, NAVER처럼 enum 이름을 그대로 보여줍니다.
        return name();
    }
}
