package com.socialogin.module.global.oauth;

import com.socialogin.module.global.exception.ErrorCode;
import com.socialogin.module.global.exception.OAuthLoginException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * application.yml의 oauth.* 설정을 Java 객체로 읽어오는 클래스입니다.
 *
 * <p>SOLID 관점:
 * - 설정을 읽는 책임만 가집니다. 실제 OAuth 호출은 OAuthClient가 담당합니다.
 * - provider가 추가되어도 Service 코드는 그대로 두고 설정과 Client만 추가하면 됩니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    // yml의 oauth.providers.google/kakao/... 값들이 이 Map에 들어옵니다.
    private Map<String, Provider> providers = new HashMap<>();

    public Provider getProvider(OAuthProvider provider) {
        // enum의 registrationId("google" 같은 값)로 provider 설정을 찾습니다.
        Provider property = providers.get(provider.getRegistrationId());

        // 설정이 없으면 이후 단계에서 null로 터지지 않게 바로 친절한 예외를 던집니다.
        if (property == null) {
            throw new OAuthLoginException(
                    ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED,
                    provider.getRegistrationId() + " OAuth 설정이 존재하지 않습니다."
            );
        }

        return property;
    }

    /**
     * provider 하나에 필요한 OAuth 설정 묶음입니다.
     */
    @Getter
    @Setter
    public static class Provider {
        // 개발자 콘솔에서 발급받는 앱 ID입니다.
        private String clientId;

        // 개발자 콘솔에서 발급받는 앱 secret입니다. Kakao는 설정에 따라 비어 있을 수 있습니다.
        private String clientSecret;

        // 로그인 성공 후 provider가 code를 보내줄 프론트엔드 callback 주소입니다.
        private String redirectUri;

        // 사용자를 provider 로그인 화면으로 보낼 주소입니다.
        private String authorizationUri;

        // code를 provider access token으로 바꾸는 주소입니다.
        private String tokenUri;

        // provider access token으로 사용자 정보를 가져올 주소입니다.
        private String userInfoUri;

        // GitHub처럼 이메일 조회가 별도인 provider를 위한 주소입니다.
        private String emailUri;

        // GitHub REST API처럼 버전 헤더가 필요한 경우 쓰는 값입니다.
        private String apiVersion;

        // provider에게 요청할 권한 목록입니다.
        private List<String> scopes = new ArrayList<>();

        public void validateClientConfig(String providerName) {
            // 로그인 URL 생성과 token 교환에 꼭 필요한 값들이 모두 있는지 확인합니다.
            boolean missingRequiredConfig = !StringUtils.hasText(clientId)
                    || !StringUtils.hasText(redirectUri)
                    || !StringUtils.hasText(authorizationUri)
                    || !StringUtils.hasText(tokenUri)
                    || !StringUtils.hasText(userInfoUri);

            if (missingRequiredConfig) {
                throw new OAuthLoginException(
                        ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED,
                        providerName + " OAuth 필수 설정이 누락되었습니다."
                );
            }
        }
    }
}
