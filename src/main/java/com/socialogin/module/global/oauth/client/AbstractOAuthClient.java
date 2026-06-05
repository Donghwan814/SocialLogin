package com.socialogin.module.global.oauth.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialogin.module.global.exception.ErrorCode;
import com.socialogin.module.global.exception.OAuthLoginException;
import com.socialogin.module.global.oauth.OAuthProperties;
import com.socialogin.module.global.oauth.userinfo.OAuthUserInfo;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * OAuthClient 구현체들의 공통 HTTP 로직을 재사용하는 추상 클래스입니다.
 *
 * <p>역할:
 * - 로그인 URL 생성, Access Token 교환, Bearer user-info 요청의 중복을 제거합니다.
 * - provider별 구현체는 "응답 Map을 어떤 OAuthUserInfo로 바꿀지"에만 집중합니다.
 *
 * <p>메모리/성능 관점:
 * - RestClient는 Client Bean 생성 시 한 번만 만들어 재사용합니다.
 * - 요청마다 필요한 작은 Map/DTO만 생성하고 원본 응답 Map은 UserInfo 변환 후 버립니다.
 */
public abstract class AbstractOAuthClient implements OAuthClient {
    // RestClient가 JSON 객체 응답을 Map<String, Object>로 역직렬화할 때 사용할 타입 정보입니다.
    protected static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    // provider 에러 응답 body를 파싱하기 위한 Jackson ObjectMapper입니다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Jackson으로 JSON 객체를 Map으로 읽을 때 사용할 타입 정보입니다.
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    // application.yml의 oauth.providers.{provider} 설정 중 이 Client가 사용할 provider 설정입니다.
    private final OAuthProperties.Provider properties;

    // provider authorization/token/user-info API를 호출하기 위한 Spring RestClient입니다.
    private final RestClient restClient;

    protected AbstractOAuthClient(OAuthProperties.Provider properties) {
        // 생성자에서 provider 설정을 받아 필드에 보관합니다.
        this.properties = properties;

        // 대부분 provider가 JSON 응답을 주므로 Accept: application/json을 기본 헤더로 둡니다.
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * provider 로그인 페이지로 이동할 URL을 만듭니다.
     *
     * <p>state는 CSRF 방어를 위해 실무에서 반드시 검증 저장소와 함께 사용하는 값입니다.
     */
    @Override
    public String generateLoginUrl(String state) {
        // client-id, redirect-uri, authorize/token/user-info URI가 없으면 실제 호출 전에 명확한 예외를 던집니다.
        properties.validateClientConfig(getProvider().getRegistrationId());

        // OAuth2 Authorization Code Flow의 첫 단계 URL을 provider 공식 endpoint 기준으로 조립합니다.
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
                // response_type=code는 "토큰을 바로 받지 말고 authorization code를 먼저 받겠다"는 의미입니다.
                .queryParam("response_type", "code")
                // client_id는 provider 개발자 콘솔에서 발급받은 앱 식별자입니다.
                .queryParam("client_id", properties.getClientId())
                // redirect_uri는 provider 로그인이 끝난 뒤 code를 받을 프론트엔드 callback 주소입니다.
                .queryParam("redirect_uri", properties.getRedirectUri());

        // scope는 사용자에게 어떤 정보 접근 권한을 요청할지 나타냅니다. provider별 구분자는 getScopeDelimiter()가 결정합니다.
        if (properties.getScopes() != null && !properties.getScopes().isEmpty()) {
            builder.queryParam("scope", String.join(getScopeDelimiter(), properties.getScopes()));
        }

        // state는 요청 위조 방지와 요청-응답 짝 맞추기에 쓰이는 임의 문자열입니다.
        if (StringUtils.hasText(state)) {
            builder.queryParam("state", state);
        }

        // query parameter를 URL-safe하게 인코딩한 최종 로그인 URL을 반환합니다.
        return builder.build().encode().toUriString();
    }

    /**
     * authorization code를 provider Access Token으로 교환합니다.
     */
    @Override
    public String requestAccessToken(String code, String state) {
        // authorization code가 없으면 token endpoint에 보낼 수 없으므로 도메인 예외로 중단합니다.
        if (!StringUtils.hasText(code)) {
            throw new OAuthLoginException(ErrorCode.MISSING_AUTHORIZATION_CODE);
        }

        try {
            // provider token endpoint로 application/x-www-form-urlencoded POST 요청을 보냅니다.
            Map<String, Object> response = restClient.post()
                    // 예: Google은 https://oauth2.googleapis.com/token 입니다.
                    .uri(properties.getTokenUri())
                    // OAuth2 token endpoint는 일반적으로 form-urlencoded body를 받습니다.
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    // grant_type, client_id, redirect_uri, code, client_secret 등을 form body로 만듭니다.
                    .body(createTokenRequestBody(code, state))
                    // retrieve()는 HTTP 응답을 받아 body 변환 단계로 넘기는 RestClient API입니다.
                    .retrieve()
                    // JSON 객체 응답을 Map으로 변환합니다. access_token, token_type 등이 여기에 들어 있습니다.
                    .body(MAP_RESPONSE_TYPE);

            // GitHub 등 일부 provider는 토큰 교환 실패도 HTTP 200 + {"error":"..."} 형태로 돌려줍니다.
            // 이 경우 access_token이 없으므로, body의 error를 그대로 노출해 진짜 실패 원인을 알 수 있게 합니다.
            throwIfProviderReturnedError(response);

            // provider 응답에서 access_token만 추출합니다. 우리 서비스 JWT가 아니라 provider API 호출용 토큰입니다.
            String accessToken = response == null ? null : String.valueOf(response.get("access_token"));

            // access_token이 없으면 provider token 교환이 실패한 것이므로 502 계열 예외로 처리합니다.
            if (!StringUtils.hasText(accessToken) || "null".equals(accessToken)) {
                throw new OAuthLoginException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
            }

            // 사용자 정보 조회 단계에서 사용할 provider access token을 반환합니다.
            return accessToken;
        } catch (OAuthLoginException exception) {
            // 우리가 의도적으로 던진 도메인 예외는 그대로 다시 던져서 에러 코드가 유지되게 합니다.
            throw exception;
        } catch (RestClientResponseException exception) {
            // provider가 4xx/5xx 응답을 내려준 경우에는 상태와 에러 body를 해석해 더 구체적인 메시지를 만듭니다.
            throw createTokenExchangeException(exception);
        } catch (RestClientException exception) {
            // 네트워크 오류, 4xx/5xx 응답 등 외부 통신 실패는 표준 OAuthLoginException으로 감쌉니다.
            throw new OAuthLoginException(
                    ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED,
                    getProvider().getRegistrationId() + " 토큰 교환에 실패했습니다.",
                    exception
            );
        }
    }

    /**
     * provider가 HTTP 200으로 돌려준 token 교환 에러(GitHub 스타일)를 의미 있는 예외로 변환합니다.
     *
     * <p>GitHub는 잘못된 code/secret도 200 + {"error":"...","error_description":"..."}로 응답하므로,
     * access_token이 없다고 일반화하지 않고 provider가 알려준 실제 원인을 노출합니다.
     */
    private void throwIfProviderReturnedError(Map<String, Object> response) {
        // 응답이 없거나 error 필드가 없으면 정상 흐름이므로 그냥 통과합니다.
        if (response == null || response.get("error") == null) {
            return;
        }

        // provider가 준 에러 코드와 부가 설명을 꺼냅니다.
        String providerError = String.valueOf(response.get("error"));
        String description = stringValue(response, "error_description");

        // client_id/client_secret 문제는 인증 실패로, 그 외(code 만료/재사용/redirect_uri 불일치 등)는 잘못된 인가코드로 매핑합니다.
        ErrorCode errorCode = "incorrect_client_credentials".equals(providerError)
                ? ErrorCode.SOCIAL_AUTHENTICATION_FAILED
                : ErrorCode.INVALID_AUTHORIZATION_CODE;

        // provider가 알려준 실제 원인을 메시지에 담아 디버깅이 쉽도록 합니다.
        String message = getProvider().getRegistrationId() + " 인가코드 교환에 실패했습니다. provider 응답: "
                + providerError
                + (StringUtils.hasText(description) ? " (" + abbreviate(description) + ")" : "");

        throw new OAuthLoginException(errorCode, message);
    }

    /**
     * provider token endpoint 실패 응답을 우리 서비스 예외로 변환합니다.
     */
    private OAuthLoginException createTokenExchangeException(RestClientResponseException exception) {
        // 4xx는 대체로 code 만료/재사용/redirect_uri 불일치/client 인증 실패이고, 5xx는 provider 쪽 장애로 봅니다.
        ErrorCode errorCode = resolveTokenExchangeErrorCode(exception);

        // 사용자가 바로 점검할 수 있는 원인을 포함한 메시지를 만듭니다.
        String message = getProvider().getRegistrationId() + " 인가코드 교환에 실패했습니다. "
                + createTokenExchangeGuideMessage(errorCode)
                + extractProviderResponseMessage(exception.getResponseBodyAsString());

        return new OAuthLoginException(errorCode, message, exception);
    }

    /**
     * provider HTTP status를 우리 서비스 에러 코드로 매핑합니다.
     */
    private ErrorCode resolveTokenExchangeErrorCode(RestClientResponseException exception) {
        // client_id/client_secret 오류는 인증 실패에 가깝습니다.
        if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
            return ErrorCode.SOCIAL_AUTHENTICATION_FAILED;
        }

        // code 만료, code 재사용, redirect_uri 불일치 같은 문제는 잘못된 authorization code 요청으로 봅니다.
        if (exception.getStatusCode().is4xxClientError()) {
            return ErrorCode.INVALID_AUTHORIZATION_CODE;
        }

        // provider 5xx 또는 기타 예상 밖의 응답은 외부 token endpoint 실패로 봅니다.
        return ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED;
    }

    /**
     * token 교환 실패 시 점검할 내용을 에러 메시지에 담습니다.
     */
    private String createTokenExchangeGuideMessage(ErrorCode errorCode) {
        if (errorCode == ErrorCode.SOCIAL_AUTHENTICATION_FAILED) {
            return "client_id, client_secret, provider 앱 설정을 확인해주세요.";
        }

        if (errorCode == ErrorCode.INVALID_AUTHORIZATION_CODE) {
            return "인가코드가 만료되었거나 이미 사용되었거나 authorize 요청의 redirect_uri와 token 요청의 redirect_uri가 다를 수 있습니다.";
        }

        return "provider token endpoint 응답이 정상적이지 않습니다.";
    }

    /**
     * provider가 내려준 에러 메시지를 안전하게 추출합니다.
     */
    private String extractProviderResponseMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "";
        }

        try {
            Map<String, Object> body = OBJECT_MAPPER.readValue(responseBody, MAP_TYPE);
            String message = readProviderMessage(body);

            if (StringUtils.hasText(message)) {
                return " provider 응답: " + abbreviate(message);
            }
        } catch (Exception ignored) {
            // provider마다 에러 body 구조가 다르므로 파싱 실패 시 상세 응답은 숨기고 표준 안내만 반환합니다.
        }

        return "";
    }

    /**
     * Facebook, Google, Naver처럼 서로 다른 에러 body 구조에서 메시지를 꺼냅니다.
     */
    private String readProviderMessage(Map<String, Object> body) {
        Object error = body.get("error");

        if (error instanceof Map<?, ?> errorMap) {
            return stringValue(errorMap, "message");
        }

        String errorDescription = stringValue(body, "error_description");
        if (StringUtils.hasText(errorDescription)) {
            return errorDescription;
        }

        if (error instanceof String errorCode) {
            return errorCode;
        }

        return stringValue(body, "message");
    }

    /**
     * Map 값이 문자열이면 꺼내고 아니면 String.valueOf로 안전하게 변환합니다.
     */
    private String stringValue(Map<?, ?> values, String key) {
        Object value = values.get(key);

        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    /**
     * provider 메시지가 너무 길 때 응답이 과하게 커지지 않도록 줄입니다.
     */
    private String abbreviate(String value) {
        int maxLength = 300;

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "...";
    }

    /**
     * provider Access Token으로 사용자 정보를 조회한 뒤 공통 OAuthUserInfo로 변환합니다.
     */
    @Override
    public OAuthUserInfo requestUserInfo(String accessToken) {
        // provider user-info endpoint를 호출해 원본 JSON 응답을 Map으로 받습니다.
        Map<String, Object> attributes = requestJsonObject(properties.getUserInfoUri(), accessToken);

        // provider별 JSON 구조를 공통 OAuthUserInfo로 변환합니다.
        return convertToUserInfo(attributes, accessToken);
    }

    /**
     * token 요청 form body를 만듭니다. 대부분 provider가 같은 OAuth2 표준 파라미터를 사용합니다.
     */
    protected MultiValueMap<String, String> createTokenRequestBody(String code, String state) {
        // form-urlencoded body는 key 하나에 여러 값을 담을 수 있어 MultiValueMap을 사용합니다.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        // authorization_code grant를 사용한다는 OAuth2 표준 파라미터입니다.
        form.add("grant_type", "authorization_code");

        // provider가 어떤 앱의 요청인지 확인할 수 있게 client_id를 보냅니다.
        form.add("client_id", properties.getClientId());

        // authorize 요청 때 사용한 redirect_uri와 token 요청의 redirect_uri가 일치해야 하는 provider가 많습니다.
        form.add("redirect_uri", properties.getRedirectUri());

        // callback으로 받은 authorization code입니다. 이 값을 provider access token으로 교환합니다.
        form.add("code", code);

        // public client가 아닌 서버 앱은 보통 client_secret도 함께 보내 앱을 인증합니다. Kakao처럼 선택인 provider도 있습니다.
        if (StringUtils.hasText(properties.getClientSecret())) {
            form.add("client_secret", properties.getClientSecret());
        }

        // 기본 form을 반환합니다. provider별 추가 파라미터가 필요하면 하위 클래스가 override합니다.
        return form;
    }

    /**
     * Bearer 인증이 필요한 JSON 객체 응답을 조회합니다.
     */
    protected Map<String, Object> requestJsonObject(String uri, String accessToken) {
        try {
            // user-info endpoint에 GET 요청을 보냅니다.
            Map<String, Object> response = restClient.get()
                    // provider별 user-info URI입니다.
                    .uri(uri)
                    // 공식 문서들이 공통적으로 사용하는 Bearer token 인증 방식입니다.
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    // HTTP 응답을 받아 body 변환 단계로 넘깁니다.
                    .retrieve()
                    // JSON 객체를 Map<String, Object>로 변환합니다.
                    .body(MAP_RESPONSE_TYPE);

            // 비어 있는 응답은 사용자 프로필 매핑을 할 수 없으므로 실패로 봅니다.
            if (response == null || response.isEmpty()) {
                throw new OAuthLoginException(ErrorCode.SOCIAL_USER_INFO_REQUEST_FAILED);
            }

            // provider별 UserInfo 변환에 사용할 원본 속성 Map을 반환합니다.
            return response;
        } catch (OAuthLoginException exception) {
            // 이미 의미 있는 에러 코드가 붙은 예외는 그대로 유지합니다.
            throw exception;
        } catch (RestClientException exception) {
            // 외부 API 호출 실패는 클라이언트에게 provider 연동 실패로 알려줍니다.
            throw new OAuthLoginException(
                    ErrorCode.SOCIAL_USER_INFO_REQUEST_FAILED,
                    getProvider().getRegistrationId() + " 사용자 정보 요청에 실패했습니다.",
                    exception
            );
        }
    }

    protected OAuthProperties.Provider getProperties() {
        // 하위 클래스가 email-uri, api-version 같은 provider 확장 설정을 읽을 수 있게 protected getter를 제공합니다.
        return properties;
    }

    /**
     * OAuth2 표준은 space-delimited scope를 쓰지만 일부 provider 공식 문서는 다른 구분자를 사용합니다.
     */
    protected String getScopeDelimiter() {
        // OAuth2 표준과 Google/GitHub는 scope를 공백으로 연결합니다.
        return " ";
    }

    /**
     * provider별 JSON 응답을 공통 OAuthUserInfo로 변환하는 확장 지점입니다.
     */
    protected abstract OAuthUserInfo convertToUserInfo(Map<String, Object> attributes, String accessToken);
}
