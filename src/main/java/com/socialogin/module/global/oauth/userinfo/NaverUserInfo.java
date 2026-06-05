package com.socialogin.module.global.oauth.userinfo;

import com.socialogin.module.global.oauth.OAuthProvider;

import java.util.Map;

/**
 * Naver user-info 응답을 우리 서비스의 공통 사용자 정보로 변환합니다.
 *
 * <p>역할:
 * - Naver는 최상위 response 객체 안에 id, email, name을 담아 내려주므로 그 구조만 담당합니다.
 * - provider별 응답 차이를 UserInfo 클래스에 숨겨 공통 로그인 흐름을 단순하게 유지합니다.
 */
public record NaverUserInfo(
        // Naver profile API의 response.id 값입니다. 네이버 아이디 문자열 자체는 제공되지 않습니다.
        String providerId,

        // response.email 값입니다.
        String email,

        // response.name 값을 닉네임으로 사용합니다.
        String nickname
) implements OAuthUserInfo {
    /**
     * Naver user-info 응답 Map을 필요한 값만 가진 record로 축소합니다.
     */
    public static NaverUserInfo from(Map<String, Object> attributes) {
        // Naver 프로필 API는 최상위 response 객체 안에 사용자 정보를 담습니다.
        Map<String, Object> response = OAuthAttributes.mapValue(attributes, "response");

        // 필요한 필드만 공통 record로 축소합니다.
        return new NaverUserInfo(
                // 애플리케이션별 유니크한 회원 식별값입니다.
                OAuthAttributes.stringValue(response, "id"),
                // 사용자 이메일입니다.
                OAuthAttributes.stringValue(response, "email"),
                // 사용자 이름입니다.
                OAuthAttributes.stringValue(response, "name")
        );
    }

    @Override
    public OAuthProvider provider() {
        // 이 UserInfo가 Naver에서 온 사용자 정보임을 알려줍니다.
        return OAuthProvider.NAVER;
    }
}
