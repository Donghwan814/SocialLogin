package com.socialogin.module.global.oauth.client;

import com.socialogin.module.global.oauth.OAuthProperties;
import com.socialogin.module.global.oauth.OAuthProvider;
import com.socialogin.module.global.oauth.userinfo.KakaoUserInfo;
import com.socialogin.module.global.oauth.userinfo.OAuthUserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kakao OAuth2 API와 통신하는 Client입니다.
 *
 * <p>역할:
 * - Kakao token/user-info API 호출을 담당합니다.
 * - Kakao의 중첩 응답 구조는 KakaoUserInfo로 변환합니다.
 */
@Component
public class KakaoOAuthClient extends AbstractOAuthClient {
    public KakaoOAuthClient(OAuthProperties properties) {
        // application.yml의 oauth.providers.kakao 설정을 읽어 공통 OAuth 로직에 전달합니다.
        super(properties.getProvider(OAuthProvider.KAKAO));
    }

    @Override
    public OAuthProvider getProvider() {
        // Factory가 kakao 요청을 이 Client로 연결할 수 있게 provider enum을 반환합니다.
        return OAuthProvider.KAKAO;
    }

    /**
     * Kakao REST API 문서의 추가 동의 scope 예시는 comma-delimited 형식을 사용합니다.
     */
    @Override
    protected String getScopeDelimiter() {
        // Kakao REST API 문서의 scope 예시는 account_email,profile_nickname처럼 comma로 연결합니다.
        return ",";
    }

    /**
     * Kakao 응답 필드를 공통 OAuthUserInfo로 변환합니다.
     */
    @Override
    protected OAuthUserInfo convertToUserInfo(Map<String, Object> attributes, String accessToken) {
        // Kakao 응답은 kakao_account/profile 중첩 구조가 있으므로 전용 파서로 변환합니다.
        return KakaoUserInfo.from(attributes);
    }
}
