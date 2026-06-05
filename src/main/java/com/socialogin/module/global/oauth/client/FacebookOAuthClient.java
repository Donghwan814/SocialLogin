package com.socialogin.module.global.oauth.client;

import com.socialogin.module.global.oauth.OAuthProperties;
import com.socialogin.module.global.oauth.OAuthProvider;
import com.socialogin.module.global.oauth.userinfo.FacebookUserInfo;
import com.socialogin.module.global.oauth.userinfo.OAuthUserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Facebook OAuth2 API와 통신하는 Client입니다.
 *
 * <p>역할:
 * - Facebook Graph API token/user-info 호출을 담당합니다.
 * - user-info-uri에 fields=id,name,email을 포함해 필요한 필드만 요청합니다.
 */
@Component
public class FacebookOAuthClient extends AbstractOAuthClient {
    public FacebookOAuthClient(OAuthProperties properties) {
        // application.yml의 oauth.providers.facebook 설정을 읽어 공통 OAuth 로직에 전달합니다.
        super(properties.getProvider(OAuthProvider.FACEBOOK));
    }

    @Override
    public OAuthProvider getProvider() {
        // Factory가 facebook 요청을 이 Client로 연결할 수 있게 provider enum을 반환합니다.
        return OAuthProvider.FACEBOOK;
    }

    /**
     * Facebook Login 예시는 permission scope를 comma-delimited 형식으로 전달합니다.
     */
    @Override
    protected String getScopeDelimiter() {
        // Facebook Login 문서 예시는 email,public_profile처럼 permission을 comma로 연결합니다.
        return ",";
    }

    /**
     * Facebook 응답 필드를 공통 OAuthUserInfo로 변환합니다.
     */
    @Override
    protected OAuthUserInfo convertToUserInfo(Map<String, Object> attributes, String accessToken) {
        // Graph API /me?fields=id,name,email 응답을 공통 사용자 정보로 변환합니다.
        return FacebookUserInfo.from(attributes);
    }
}
