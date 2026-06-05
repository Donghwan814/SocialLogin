package com.socialogin.module.global.oauth.userinfo;

import com.socialogin.module.global.oauth.OAuthProvider;

import java.util.Map;

/**
 * Facebook user-info 응답을 우리 서비스의 공통 사용자 정보로 변환합니다.
 *
 * <p>역할:
 * - Facebook Graph API의 id, email, name 필드를 공통 모델로 변환합니다.
 * - user-info-uri에 fields=id,name,email을 포함하면 이 클래스는 응답 파싱만 담당합니다.
 */
public record FacebookUserInfo(
        // Facebook Graph API /me 응답의 id 값입니다.
        String providerId,

        // fields=id,name,email 요청으로 받은 email 값입니다. 사용자가 이메일 제공을 거부하면 없을 수 있습니다.
        String email,

        // fields=id,name,email 요청으로 받은 name 값입니다.
        String nickname
) implements OAuthUserInfo {
    /**
     * Facebook user-info 응답 Map을 필요한 값만 가진 record로 축소합니다.
     */
    public static FacebookUserInfo from(Map<String, Object> attributes) {
        // Graph API 응답에서 필요한 필드만 꺼내 공통 record로 변환합니다.
        return new FacebookUserInfo(
                // Facebook 사용자 식별자입니다.
                OAuthAttributes.stringValue(attributes, "id"),
                // Facebook 계정 이메일입니다.
                OAuthAttributes.stringValue(attributes, "email"),
                // Facebook 사용자 이름입니다.
                OAuthAttributes.stringValue(attributes, "name")
        );
    }

    @Override
    public OAuthProvider provider() {
        // 이 UserInfo가 Facebook에서 온 사용자 정보임을 알려줍니다.
        return OAuthProvider.FACEBOOK;
    }
}
