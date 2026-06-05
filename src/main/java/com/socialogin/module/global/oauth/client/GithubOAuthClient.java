package com.socialogin.module.global.oauth.client;

import com.socialogin.module.global.exception.ErrorCode;
import com.socialogin.module.global.exception.OAuthLoginException;
import com.socialogin.module.global.oauth.OAuthProperties;
import com.socialogin.module.global.oauth.OAuthProvider;
import com.socialogin.module.global.oauth.userinfo.GithubUserInfo;
import com.socialogin.module.global.oauth.userinfo.OAuthUserInfo;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * GitHub OAuth2 API와 통신하는 Client입니다.
 *
 * <p>역할:
 * - GitHub token/user-info API 호출을 담당합니다.
 * - GitHub /user 응답의 email이 null일 수 있으므로 /user/emails 보조 API로 primary email을 보정합니다.
 */
@Component
public class GithubOAuthClient extends AbstractOAuthClient {
    // GitHub REST API 공식 예시에서 권장하는 vendor media type입니다.
    private static final String GITHUB_ACCEPT = "application/vnd.github+json";

    // GitHub REST API 버전을 명시하기 위한 공식 헤더 이름입니다.
    private static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";

    // /user/emails 응답은 JSON 배열이므로 List<Map<String, Object>> 타입 정보가 필요합니다.
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    // GitHub 전용 헤더를 넣어 호출하기 위해 별도 RestClient를 보관합니다.
    private final RestClient restClient;

    public GithubOAuthClient(OAuthProperties properties) {
        // application.yml의 oauth.providers.github 설정을 읽어 공통 OAuth 로직에 전달합니다.
        super(properties.getProvider(OAuthProvider.GITHUB));

        // GitHub API 호출에 사용할 RestClient입니다.
        this.restClient = RestClient.builder().build();
    }

    @Override
    public OAuthProvider getProvider() {
        // Factory가 github 요청을 이 Client로 연결할 수 있게 provider enum을 반환합니다.
        return OAuthProvider.GITHUB;
    }

    /**
     * GitHub REST API 공식 예시에 맞춰 vendor Accept와 API version header를 함께 전송합니다.
     */
    @Override
    protected Map<String, Object> requestJsonObject(String uri, String accessToken) {
        try {
            // GitHub /user endpoint 호출을 준비합니다.
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    // user-info-uri는 기본적으로 https://api.github.com/user 입니다.
                    .uri(uri)
                    // OAuth access token을 Bearer 방식으로 보냅니다.
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    // GitHub REST API 공식 media type을 Accept 헤더에 넣습니다.
                    .header(HttpHeaders.ACCEPT, GITHUB_ACCEPT);

            // 설정에 api-version이 있으면 X-GitHub-Api-Version 헤더도 추가합니다.
            request = addGitHubApiVersionHeader(request);

            // 응답 JSON 객체를 Map으로 변환합니다.
            Map<String, Object> response = request.retrieve().body(MAP_RESPONSE_TYPE);

            // 사용자 프로필이 비어 있으면 회원 매핑이 불가능하므로 실패로 봅니다.
            if (response == null || response.isEmpty()) {
                throw new OAuthLoginException(ErrorCode.SOCIAL_USER_INFO_REQUEST_FAILED);
            }

            // GitHubUserInfo 변환에 사용할 원본 Map을 반환합니다.
            return response;
        } catch (OAuthLoginException exception) {
            // 의미 있는 에러 코드를 가진 예외는 그대로 유지합니다.
            throw exception;
        } catch (RestClientException exception) {
            // 외부 API 통신 실패는 표준 소셜 사용자 정보 요청 실패로 감쌉니다.
            throw new OAuthLoginException(
                    ErrorCode.SOCIAL_USER_INFO_REQUEST_FAILED,
                    "GitHub 사용자 정보 요청에 실패했습니다.",
                    exception
            );
        }
    }

    /**
     * GitHub 응답 필드를 공통 OAuthUserInfo로 변환합니다.
     *
     * <p>email이 비어 있으면 email-uri 설정을 사용해 primary/verified 이메일을 추가 조회합니다.
     */
    @Override
    protected OAuthUserInfo convertToUserInfo(Map<String, Object> attributes, String accessToken) {
        // GitHub /user 응답의 email은 사용자가 이메일을 비공개로 하면 null일 수 있습니다.
        String email = attributes.get("email") == null ? null : String.valueOf(attributes.get("email"));

        // email이 비어 있으면 user:email scope로 허용된 /user/emails API를 추가 호출합니다.
        if (!StringUtils.hasText(email)) {
            email = requestPrimaryEmail(accessToken);
        }

        // 보정된 email과 /user 응답을 공통 OAuthUserInfo로 변환합니다.
        return GithubUserInfo.from(attributes, email);
    }

    /**
     * GitHub private email 보정을 위해 /user/emails를 호출합니다.
     */
    private String requestPrimaryEmail(String accessToken) {
        // email-uri는 기본적으로 https://api.github.com/user/emails 입니다.
        String emailUri = getProperties().getEmailUri();

        // 설정이 없다면 이메일 보정 호출을 할 수 없으므로 null을 반환합니다.
        if (!StringUtils.hasText(emailUri)) {
            return null;
        }

        try {
            // GitHub 이메일 목록 조회 요청을 준비합니다.
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    // 이메일 목록 endpoint입니다.
                    .uri(emailUri)
                    // 동일한 provider access token으로 인증합니다.
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    // GitHub REST API 공식 media type을 사용합니다.
                    .header(HttpHeaders.ACCEPT, GITHUB_ACCEPT);

            // 설정에 api-version이 있으면 버전 헤더를 추가합니다.
            request = addGitHubApiVersionHeader(request);

            // 응답은 이메일 객체 배열이므로 List<Map<String, Object>>로 받습니다.
            List<Map<String, Object>> emails = request.retrieve().body(LIST_MAP_RESPONSE_TYPE);

            // 응답이 없으면 보정할 이메일도 없으므로 null을 반환합니다.
            if (emails == null) {
                return null;
            }

            // primary=true이고 verified=true인 이메일을 우선 사용합니다.
            return emails.stream()
                    // GitHub 계정의 대표 이메일만 후보로 남깁니다.
                    .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                    // 검증된 이메일만 사용해 가입 이메일 신뢰도를 높입니다.
                    .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                    // Map에서 email 문자열만 꺼냅니다.
                    .map(email -> String.valueOf(email.get("email")))
                    // 빈 문자열은 제외합니다.
                    .filter(StringUtils::hasText)
                    // 첫 번째 후보를 반환합니다.
                    .findFirst()
                    // 후보가 없으면 null을 반환합니다.
                    .orElse(null);
        } catch (RestClientException exception) {
            // 이메일 보정 API 실패도 사용자 정보 요청 실패 범주로 처리합니다.
            throw new OAuthLoginException(
                    ErrorCode.SOCIAL_USER_INFO_REQUEST_FAILED,
                    "GitHub 이메일 정보 요청에 실패했습니다.",
                    exception
            );
        }
    }

    private RestClient.RequestHeadersSpec<?> addGitHubApiVersionHeader(RestClient.RequestHeadersSpec<?> request) {
        // application.yml에서 api-version을 설정했는지 확인합니다.
        if (StringUtils.hasText(getProperties().getApiVersion())) {
            // GitHub 공식 REST API 버전 헤더를 요청에 추가합니다.
            return request.header(GITHUB_API_VERSION_HEADER, getProperties().getApiVersion());
        }

        // 버전 설정이 없으면 원래 요청을 그대로 반환합니다.
        return request;
    }
}
