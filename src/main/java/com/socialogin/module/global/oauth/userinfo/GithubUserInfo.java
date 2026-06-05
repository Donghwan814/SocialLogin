package com.socialogin.module.global.oauth.userinfo;

import com.socialogin.module.global.oauth.OAuthProvider;

import java.util.Map;

/**
 * GitHub user-info 응답을 우리 서비스의 공통 사용자 정보로 변환합니다.
 *
 * <p>역할:
 * - GitHub의 id, email, name/login 필드를 공통 모델로 변환합니다.
 * - GitHub는 이메일이 비공개일 수 있어 OAuthClient에서 /user/emails 보조 API를 호출한 뒤 email을 채워 넣습니다.
 */
public record GithubUserInfo(
        // GitHub /user 응답의 id 값입니다.
        String providerId,

        // /user의 email 또는 /user/emails에서 보정한 primary verified email입니다.
        String email,

        // /user의 name이 있으면 name, 없으면 login을 닉네임으로 사용합니다.
        String nickname
) implements OAuthUserInfo {
    /**
     * GitHub user-info 응답 Map과 보정된 이메일 값을 사용해 record를 만듭니다.
     */
    public static GithubUserInfo from(Map<String, Object> attributes, String resolvedEmail) {
        // GitHub 사용자가 프로필 이름을 설정하지 않으면 name이 null일 수 있습니다.
        String name = OAuthAttributes.stringValue(attributes, "name");

        // login은 GitHub username이라 name이 없을 때 대체 닉네임으로 쓰기 좋습니다.
        String login = OAuthAttributes.stringValue(attributes, "login");

        // name이 비어 있으면 login을 사용해 nickname not-null 요구를 만족시킵니다.
        String nickname = name == null || name.isBlank() ? login : name;

        // 필요한 값만 공통 record로 변환합니다.
        return new GithubUserInfo(
                // GitHub 사용자 식별자입니다.
                OAuthAttributes.stringValue(attributes, "id"),
                // 보정된 이메일입니다.
                resolvedEmail,
                // name 또는 login입니다.
                nickname
        );
    }

    @Override
    public OAuthProvider provider() {
        // 이 UserInfo가 GitHub에서 온 사용자 정보임을 알려줍니다.
        return OAuthProvider.GITHUB;
    }
}
