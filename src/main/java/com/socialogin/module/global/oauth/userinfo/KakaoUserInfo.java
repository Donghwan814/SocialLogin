package com.socialogin.module.global.oauth.userinfo;

import com.socialogin.module.global.oauth.OAuthProvider;

import java.util.Map;

/**
 * Kakao user-info 응답을 우리 서비스의 공통 사용자 정보로 변환합니다.
 *
 * <p>역할:
 * - Kakao의 id, kakao_account.email, kakao_account.profile.nickname 구조를 파싱합니다.
 * - 중첩 구조 파싱을 이 클래스에 가둬 OAuthService가 Kakao JSON을 알 필요 없게 합니다.
 */
public record KakaoUserInfo(
        // Kakao의 id 값입니다. 숫자일 수 있어 문자열로 변환해서 저장합니다.
        String providerId,

        // kakao_account.email 값입니다. account_email scope 동의가 필요합니다.
        String email,

        // kakao_account.profile.nickname 값입니다. profile_nickname scope 동의가 필요합니다.
        String nickname
) implements OAuthUserInfo {
    /**
     * Kakao user-info 응답 Map을 필요한 값만 가진 record로 축소합니다.
     */
    public static KakaoUserInfo from(Map<String, Object> attributes) {
        // Kakao 응답의 kakao_account 중첩 객체를 꺼냅니다.
        Map<String, Object> kakaoAccount = OAuthAttributes.mapValue(attributes, "kakao_account");

        // kakao_account 안의 profile 중첩 객체를 꺼냅니다.
        Map<String, Object> profile = OAuthAttributes.mapValue(kakaoAccount, "profile");

        // 필요한 필드만 공통 record로 축소합니다.
        return new KakaoUserInfo(
                // 최상위 id가 Kakao 사용자 식별자입니다.
                OAuthAttributes.stringValue(attributes, "id"),
                // 이메일은 kakao_account 안에 있습니다.
                OAuthAttributes.stringValue(kakaoAccount, "email"),
                // 닉네임은 kakao_account.profile 안에 있습니다.
                OAuthAttributes.stringValue(profile, "nickname")
        );
    }

    @Override
    public OAuthProvider provider() {
        // 이 UserInfo가 Kakao에서 온 사용자 정보임을 알려줍니다.
        return OAuthProvider.KAKAO;
    }
}
