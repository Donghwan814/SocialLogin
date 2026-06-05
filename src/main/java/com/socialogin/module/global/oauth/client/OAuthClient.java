package com.socialogin.module.global.oauth.client;

import com.socialogin.module.global.oauth.OAuthProvider;
import com.socialogin.module.global.oauth.userinfo.OAuthUserInfo;

/**
 * 소셜 provider와 통신하는 공통 인터페이스입니다.
 *
 * <p>역할:
 * - 로그인 URL 생성, authorization code로 provider Access Token 요청, user-info 조회를 추상화합니다.
 * - OAuthService가 Google/Kakao/Naver 같은 구현체를 직접 알지 않아도 되게 합니다.
 *
 * <p>provider가 추가될 때:
 * - 이 인터페이스를 구현하는 새 Client Bean을 추가합니다.
 * - OAuthClientFactory가 Bean 목록을 자동으로 Map에 등록합니다.
 */
public interface OAuthClient {
    // 이 Client가 담당하는 provider를 알려줍니다.
    OAuthProvider getProvider();

    // 클라이언트가 이동할 provider 로그인 URL을 만듭니다.
    String generateLoginUrl(String state);

    // authorization code를 provider access token으로 바꿉니다.
    // state는 일부 provider가 token 요청에서 요구할 수 있어 선택값으로 받습니다.
    String requestAccessToken(String code, String state);

    // provider access token으로 사용자 정보를 조회합니다.
    OAuthUserInfo requestUserInfo(String accessToken);
}
