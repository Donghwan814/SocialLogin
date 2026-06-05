package com.socialogin.module.global.oauth.client;

import com.socialogin.module.global.oauth.OAuthProperties;
import com.socialogin.module.global.oauth.OAuthProvider;
import com.socialogin.module.global.oauth.userinfo.NaverUserInfo;
import com.socialogin.module.global.oauth.userinfo.OAuthUserInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Naver OAuth2 API와 통신하는 Client입니다.
 *
 * <p>역할:
 * - Naver token/user-info API 호출을 담당합니다.
 * - Naver의 response 래퍼 구조는 NaverUserInfo로 변환합니다.
 */
@Component
public class NaverOAuthClient extends AbstractOAuthClient {
    public NaverOAuthClient(OAuthProperties properties) {
        // application.yml의 oauth.providers.naver 설정을 읽어 공통 OAuth 로직에 전달합니다.
        super(properties.getProvider(OAuthProvider.NAVER));
    }

    @Override
    public OAuthProvider getProvider() {
        // Factory가 naver 요청을 이 Client로 연결할 수 있게 provider enum을 반환합니다.
        return OAuthProvider.NAVER;
    }

    /**
     * state가 전달된 경우에만 Naver token 요청에 함께 보냅니다.
     */
    @Override
    protected MultiValueMap<String, String> createTokenRequestBody(String code, String state) {
        // grant_type/client_id/redirect_uri/code/client_secret 같은 공통 form body를 먼저 만듭니다.
        MultiValueMap<String, String> form = super.createTokenRequestBody(code, state);

        // 프론트엔드가 state를 백엔드로 전달하지 않는 구조에서는 이 값이 비어 있을 수 있습니다.
        if (StringUtils.hasText(state)) {
            form.add("state", state);
        }

        // state가 추가된 form body를 token endpoint 요청에 사용합니다.
        return form;
    }

    /**
     * Naver 응답 필드를 공통 OAuthUserInfo로 변환합니다.
     */
    @Override
    protected OAuthUserInfo convertToUserInfo(Map<String, Object> attributes, String accessToken) {
        // Naver 응답은 최상위 response 객체 안에 사용자 값이 있으므로 전용 파서로 변환합니다.
        return NaverUserInfo.from(attributes);
    }
}
