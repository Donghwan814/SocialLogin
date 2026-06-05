package com.socialogin.module.global.oauth.userinfo;

import com.socialogin.module.global.oauth.OAuthProvider;

/**
 * 모든 소셜 provider의 사용자 정보를 동일하게 다루기 위한 인터페이스입니다.
 *
 * <p>역할:
 * - Google, Kakao, Naver, Facebook, GitHub 응답 JSON 구조가 달라도 Service는 이 인터페이스만 바라봅니다.
 * - provider별 파싱 책임은 각 UserInfo 구현체에 가두고, 회원가입/로그인 로직은 공통화합니다.
 *
 * <p>provider가 추가될 때:
 * - 새 provider 응답을 이 인터페이스에 맞게 변환하는 UserInfo 구현체를 추가합니다.
 * - OAuthService는 수정하지 않습니다.
 */
public interface OAuthUserInfo {
    /**
     * 어떤 소셜 provider에서 온 사용자 정보인지 알려줍니다.
     */
    OAuthProvider provider();

    /**
     * provider 안에서 사용자를 구분하는 고유 ID입니다.
     */
    String providerId();

    /**
     * 우리 서비스 User.email에 저장할 이메일입니다.
     */
    String email();

    /**
     * 우리 서비스 User.name에 저장할 표시 이름입니다.
     */
    String nickname();
}
