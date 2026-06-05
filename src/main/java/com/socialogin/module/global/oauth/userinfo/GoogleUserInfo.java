package com.socialogin.module.global.oauth.userinfo;

import com.socialogin.module.global.oauth.OAuthProvider;

import java.util.Map;

/**
 * Google user-info 응답을 우리 서비스의 공통 사용자 정보로 변환합니다.
 *
 * <p>역할:
 * - Google 응답 필드(sub, email, name)를 OAuthUserInfo 인터페이스로 맞춥니다.
 * - Google 전용 JSON 구조를 Service 계층 밖으로 새어나가지 않게 합니다.
 */
public record GoogleUserInfo(
        // Google의 sub 값입니다. 같은 Google 계정과 앱 조합에서 사용자를 식별하는 값입니다.
        String providerId,

        // Google이 내려준 이메일입니다. openid/email scope가 필요합니다.
        String email,

        // Google 프로필의 name 값입니다. profile scope가 필요합니다.
        String nickname
) implements OAuthUserInfo {
    /**
     * Google user-info 응답 Map을 필요한 값만 가진 record로 축소합니다.
     */
    public static GoogleUserInfo from(Map<String, Object> attributes) {
        // provider 응답 Map에서 필요한 값만 꺼내 record로 보관합니다.
        return new GoogleUserInfo(
                // OpenID Connect userinfo의 subject 식별자입니다.
                OAuthAttributes.stringValue(attributes, "sub"),
                // 사용자 이메일입니다.
                OAuthAttributes.stringValue(attributes, "email"),
                // 사용자 표시 이름입니다.
                OAuthAttributes.stringValue(attributes, "name")
        );
    }

    @Override
    public OAuthProvider provider() {
        // 이 UserInfo가 Google에서 온 사용자 정보임을 알려줍니다.
        return OAuthProvider.GOOGLE;
    }
}
