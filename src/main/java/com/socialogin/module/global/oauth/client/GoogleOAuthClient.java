package com.socialogin.module.global.oauth.client;

import com.socialogin.module.global.oauth.OAuthProperties;
import com.socialogin.module.global.oauth.OAuthProvider;
import com.socialogin.module.global.oauth.userinfo.GoogleUserInfo;
import com.socialogin.module.global.oauth.userinfo.OAuthUserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Google OAuth2 API와 통신하는 Client입니다.
 *
 * <p>역할:
 * - Google 로그인 URL, token 교환, user-info 조회를 담당합니다.
 * - Google 응답은 GoogleUserInfo로 변환합니다.
 */
@Component
public class GoogleOAuthClient extends AbstractOAuthClient {
    public GoogleOAuthClient(OAuthProperties properties) {
        // application.yml의 oauth.providers.google 설정만 꺼내 공통 OAuthClient 로직에 넘깁니다.
        super(properties.getProvider(OAuthProvider.GOOGLE));
    }

    @Override
    public OAuthProvider getProvider() {
        // Factory가 이 구현체를 GOOGLE provider용 Client로 등록할 수 있게 알려줍니다.
        return OAuthProvider.GOOGLE;
    }

    /**
     * Google 응답 필드를 공통 OAuthUserInfo로 변환합니다.
     */
    @Override
    protected OAuthUserInfo convertToUserInfo(Map<String, Object> attributes, String accessToken) {
        // Google user-info JSON은 sub/email/name 필드를 사용하므로 GoogleUserInfo가 그 구조를 파싱합니다.
        return GoogleUserInfo.from(attributes);
    }
}
