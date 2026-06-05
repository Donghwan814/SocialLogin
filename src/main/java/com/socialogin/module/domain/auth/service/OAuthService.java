package com.socialogin.module.domain.auth.service;

import com.socialogin.module.domain.auth.dto.OAuthLoginRequest;
import com.socialogin.module.domain.auth.dto.OAuthLoginUrlResponse;
import com.socialogin.module.domain.auth.dto.TokenResponse;
import com.socialogin.module.domain.auth.entity.RefreshToken;
import com.socialogin.module.domain.auth.repository.RefreshTokenRepository;
import com.socialogin.module.domain.user.entity.User;
import com.socialogin.module.domain.user.repository.UserRepository;
import com.socialogin.module.global.exception.ErrorCode;
import com.socialogin.module.global.exception.OAuthLoginException;
import com.socialogin.module.global.jwt.JwtTokenProvider;
import com.socialogin.module.global.oauth.OAuthProvider;
import com.socialogin.module.global.oauth.client.OAuthClient;
import com.socialogin.module.global.oauth.client.OAuthClientFactory;
import com.socialogin.module.global.oauth.userinfo.OAuthUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 소셜 로그인 전체 흐름을 담당하는 Application Service입니다.
 *
 * <p>역할:
 * - 로그인 URL 생성
 * - authorization code 처리
 * - provider Access Token 요청
 * - provider user-info 조회
 * - 기존 회원 로그인 또는 신규 회원가입
 * - 자체 JWT Access Token/Refresh Token 발급
 * - Refresh Token DB 저장
 *
 * <p>구조를 이렇게 둔 이유:
 * - Controller는 HTTP 요청/응답만 담당하고 비즈니스 흐름은 Service에 둡니다.
 * - provider별 API 호출은 OAuthClient에 위임하고, JWT 발급은 JwtTokenProvider에 위임합니다.
 * - Service는 구체 provider 구현체가 아니라 OAuthClientFactory와 OAuthClient 인터페이스에 의존합니다.
 */
@Service
@RequiredArgsConstructor
public class OAuthService {
    // provider 이름(google, kakao 등)에 맞는 OAuthClient 구현체를 찾아주는 객체입니다.
    private final OAuthClientFactory oAuthClientFactory;

    // User Entity를 DB에서 조회/저장하기 위한 Spring Data JPA Repository입니다.
    private final UserRepository userRepository;

    // Refresh Token을 DB에 저장하고 기존 토큰을 삭제하기 위한 Repository입니다.
    private final RefreshTokenRepository refreshTokenRepository;

    // 우리 서비스의 JWT Access Token/Refresh Token을 생성하고 검증하는 컴포넌트입니다.
    private final JwtTokenProvider jwtTokenProvider;

    // provider 로그인 페이지 URL을 생성합니다.
    @Transactional(readOnly = true)
    public OAuthLoginUrlResponse getLoginUrl(String providerValue, String state) {
        // URL path로 들어온 문자열(providerValue)을 OAuth provider enum으로 바꿉니다.
        OAuthProvider provider = OAuthProvider.fromOAuthProvider(providerValue);

        // provider별 API 호출 규칙이 다르므로, enum에 맞는 OAuthClient 구현체를 Factory에서 꺼냅니다.
        OAuthClient client = oAuthClientFactory.getClient(provider);

        // state는 CSRF 방어에 쓰는 임의 문자열입니다. 클라이언트가 주지 않으면 서버가 UUID로 만듭니다.
        String stateValue = StringUtils.hasText(state) ? state : UUID.randomUUID().toString();

        // provider 공식 문서 형식에 맞는 authorize URL을 만듭니다.
        String loginUrl = client.generateLoginUrl(stateValue);

        // 프론트엔드가 provider, 이동할 URL, state를 모두 알 수 있도록 DTO로 감싸서 반환합니다.
        return new OAuthLoginUrlResponse(provider.getRegistrationId(), loginUrl, stateValue);
    }

    // authorization code로 로그인/회원가입을 처리하고 JWT를 발급합니다.
    @Transactional
    public TokenResponse login(String providerValue, OAuthLoginRequest request) {
        // callback 요청에 code가 없으면 provider token 교환을 할 수 없으므로 즉시 400 예외를 던집니다.
        if (request == null || !StringUtils.hasText(request.code())) {
            throw new OAuthLoginException(ErrorCode.MISSING_AUTHORIZATION_CODE);
        }

        // 문자열 provider를 OAuth provider enum으로 변환해 이후 코드에서 오타 가능성을 줄입니다.
        OAuthProvider provider = OAuthProvider.fromOAuthProvider(providerValue);

        // provider에 맞는 OAuthClient를 가져옵니다. 예: google -> GoogleOAuthClient.
        OAuthClient client = oAuthClientFactory.getClient(provider);

        // authorization code를 provider access token으로 교환합니다. state 검증은 프론트엔드 callback에서 처리하고 백엔드 body에는 code만 받습니다.
        String socialAccessToken = client.requestAccessToken(request.code(), null);

        // provider access token으로 사용자 프로필 API를 호출하고, 공통 OAuthUserInfo 형태로 변환합니다.
        OAuthUserInfo userInfo = client.requestUserInfo(socialAccessToken);

        // 우리 서비스 가입/로그인에 필요한 providerId와 email이 있는지 확인합니다.
        validateUserInfo(userInfo);

        // 이메일과 provider 기준으로 기존 회원 로그인 또는 신규 회원가입 여부를 결정합니다.
        User user = findOrCreateUser(userInfo);

        // 우리 서비스 API 호출에 사용할 짧은 수명의 JWT Access Token을 만듭니다.
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());

        // Access Token 재발급에 사용할 긴 수명의 Refresh Token을 만듭니다.
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // Refresh Token을 DB에 저장해 서버가 토큰을 추적/폐기할 수 있게 합니다.
        saveRefreshToken(user, refreshToken);

        // 프론트엔드가 token type과 만료 시간을 함께 알 수 있도록 응답 DTO를 만듭니다.
        return TokenResponse.bearer(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration(),
                jwtTokenProvider.getRefreshTokenExpiration()
        );
    }

    // provider에서 받은 사용자 정보가 회원가입에 충분한지 확인합니다.
    private void validateUserInfo(OAuthUserInfo userInfo) {
        // providerId는 provider 안에서 사용자를 구분하는 핵심 값입니다. 없으면 회원 매핑이 불가능합니다.
        // email은 provider가 주지 않을 수 있어 더 이상 필수로 강제하지 않습니다. 신원 기준은 provider+providerId입니다.
        if (userInfo == null || !StringUtils.hasText(userInfo.providerId())) {
            throw new OAuthLoginException(ErrorCode.SOCIAL_PROFILE_MAPPING_FAILED);
        }
    }

    // 기존 회원을 찾고, 없으면 새 회원을 만듭니다.
    private User findOrCreateUser(OAuthUserInfo userInfo) {
        // 소셜 로그인의 진짜 신원은 provider + providerId입니다. 이메일은 없을 수 있으므로 신원 기준으로 사용하지 않습니다.
        return userRepository.findByProviderAndProviderId(userInfo.provider(), userInfo.providerId())
                // 이미 가입된 소셜 계정이면 그대로 로그인 처리합니다.
                .orElseGet(() -> registerNewUser(userInfo));
    }

    // 신규 소셜 회원을 만듭니다. 이메일이 있을 때만 다른 provider 중복 가입을 검사합니다.
    private User registerNewUser(OAuthUserInfo userInfo) {
        // 이메일이 있을 때만, 같은 이메일이 다른 provider로 이미 가입돼 있는지 확인해 자동 계정 병합을 막습니다.
        if (StringUtils.hasText(userInfo.email())) {
            userRepository.findByEmail(userInfo.email())
                    // 같은 이메일이 다른 provider/LOCAL로 존재하면 자동 로그인을 막고 409 Conflict로 응답합니다.
                    .ifPresent(existingUser -> {
                        throw new OAuthLoginException(
                                ErrorCode.DUPLICATED_EMAIL_WITH_DIFFERENT_PROVIDER,
                                duplicatedEmailMessage(existingUser.getProvider())
                        );
                    });
        }

        // 신원이 처음 보는 소셜 계정이므로 신규 회원으로 저장합니다.
        return userRepository.save(User.createOAuthUser(userInfo));
    }

    // 기존 가입 방식에 맞는 사용자 안내 메시지를 만듭니다.
    private String duplicatedEmailMessage(OAuthProvider existingProvider) {
        // LOCAL 회원은 소셜 로그인이 아니라 이메일/비밀번호 로그인을 안내합니다.
        if (existingProvider == OAuthProvider.LOCAL) {
            return "이미 LOCAL 계정으로 가입된 이메일입니다. 이메일/비밀번호 로그인을 이용해주세요.";
        }

        // 소셜 회원은 기존 가입 provider로 다시 로그인하도록 안내합니다.
        String providerName = existingProvider.getDisplayName();
        return "이미 " + providerName + " 로그인으로 가입된 이메일입니다. " + providerName + " 로그인을 이용해주세요.";
    }

    // 사용자당 Refresh Token을 하나만 유지합니다.
    // 여러 기기 로그인을 허용하려면 deviceId 같은 값을 RefreshToken에 추가하면 됩니다.
    private void saveRefreshToken(User user, String refreshToken) {
        // 현재 설계는 사용자당 Refresh Token을 하나만 유지하므로, 새 로그인 전에 기존 토큰을 삭제합니다.
        refreshTokenRepository.deleteByUserId(user.getId());

        // JWT 문자열과 만료 시간을 RefreshToken Entity로 만들어 DB에 저장합니다.
        refreshTokenRepository.save(RefreshToken.create(user, refreshToken, jwtTokenProvider.getRefreshTokenExpiration()));
    }
}
