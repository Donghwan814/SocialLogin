package com.socialogin.module.global.jwt;

import com.socialogin.module.domain.user.entity.UserRole;
import com.socialogin.module.global.exception.ErrorCode;
import com.socialogin.module.global.exception.OAuthLoginException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 우리 서비스 JWT 생성/검증을 담당하는 클래스입니다.
 *
 * <p>역할:
 * - Access Token 생성
 * - Refresh Token 생성
 * - Token 검증
 * - Token에서 사용자 식별 정보 추출
 *
 * <p>구조를 이렇게 둔 이유:
 * - JWT 발급 로직을 Service나 Controller에 두면 인증 정책 변경 시 수정 범위가 커집니다.
 * - JwtTokenProvider에 모으면 토큰 만료 시간, claim 정책, 서명 알고리즘 변경을 한 곳에서 처리할 수 있습니다.
 */
@Component
public class JwtTokenProvider {
    // HMAC-SHA 계열 JWT 서명 키는 너무 짧으면 안전하지 않으므로 최소 길이를 강제합니다.
    private static final int HMAC_SHA_MIN_SECRET_LENGTH = 32;

    // JWT 서명/검증에 사용할 HMAC secret key입니다.
    private final SecretKey secretKey;

    // Access Token 유효 시간입니다. application.yml에서 밀리초 단위로 주입됩니다.
    private final long accessTokenExpiration;

    // Refresh Token 유효 시간입니다. application.yml에서 밀리초 단위로 주입됩니다.
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        // secret 문자열을 UTF-8 바이트로 바꿔 실제 키 길이를 확인합니다.
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

        // 키가 너무 짧으면 서버 시작 단계에서 바로 실패시켜 잘못된 보안 설정을 막습니다.
        if (secretBytes.length < HMAC_SHA_MIN_SECRET_LENGTH) {
            throw new OAuthLoginException(
                    ErrorCode.SOCIAL_TOKEN_ISSUE_FAILED,
                    "JWT_SECRET은 최소 32바이트 이상이어야 합니다."
            );
        }

        // JJWT가 사용할 수 있는 SecretKey 객체로 변환합니다.
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);

        // 주입받은 Access Token 만료 시간을 필드에 저장합니다.
        this.accessTokenExpiration = accessTokenExpiration;

        // 주입받은 Refresh Token 만료 시간을 필드에 저장합니다.
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * API 인증에 사용할 짧은 수명의 Access Token을 생성합니다.
     */
    public String createAccessToken(Long userId, String email, UserRole role) {
        // issuedAt에 넣을 현재 시각입니다.
        Date now = new Date();

        // 현재 시각 + Access Token 만료 시간으로 expiration을 계산합니다.
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        // JJWT builder로 JWT payload와 서명을 구성합니다.
        return Jwts.builder()
                // subject에는 가장 중요한 식별자인 userId를 넣습니다.
                .subject(String.valueOf(userId))
                // type claim은 Access/Refresh Token을 구분하기 위한 우리 서비스 규칙입니다.
                .claim("type", "access")
                // email은 필요한 곳에서 사용자 이메일을 빠르게 확인할 수 있게 넣습니다.
                .claim("email", email)
                // role은 Spring Security 권한 객체를 만들 때 사용합니다.
                .claim("role", role.name())
                // 토큰 발급 시각입니다.
                .issuedAt(now)
                // 토큰 만료 시각입니다.
                .expiration(expiration)
                // secretKey로 서명해 위변조를 막습니다.
                .signWith(secretKey)
                // 최종 JWT 문자열로 압축합니다.
                .compact();
    }

    /**
     * Access Token 재발급에 사용할 긴 수명의 Refresh Token을 생성합니다.
     */
    public String createRefreshToken(Long userId) {
        // Refresh Token 발급 시각입니다.
        Date now = new Date();

        // 현재 시각 + Refresh Token 만료 시간으로 expiration을 계산합니다.
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        // Refresh Token은 재발급 용도라 email/role 없이 최소 정보만 담습니다.
        return Jwts.builder()
                // subject에는 userId를 넣어 어떤 사용자의 토큰인지 알 수 있게 합니다.
                .subject(String.valueOf(userId))
                // Access Token과 구분하기 위해 type=refresh를 넣습니다.
                .claim("type", "refresh")
                // 발급 시각입니다.
                .issuedAt(now)
                // 만료 시각입니다.
                .expiration(expiration)
                // 같은 secretKey로 서명합니다.
                .signWith(secretKey)
                // 최종 JWT 문자열로 만듭니다.
                .compact();
    }

    /**
     * 서명과 만료 시간을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            // parseClaims가 성공하면 서명과 만료 시간이 유효하다는 뜻입니다.
            parseClaims(token);

            // 예외가 없으면 유효한 토큰으로 판단합니다.
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            // 서명 오류, 만료, 잘못된 형식 등은 false로 처리해 필터에서 인증하지 않게 합니다.
            return false;
        }
    }

    /**
     * 토큰 subject에서 사용자 id를 추출합니다.
     */
    public Long getUserId(String token) {
        // subject는 문자열로 저장했으므로 Long으로 변환해 반환합니다.
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /**
     * Access Token의 email claim을 추출합니다.
     */
    public String getEmail(String token) {
        // email claim을 String 타입으로 읽습니다.
        return parseClaims(token).get("email", String.class);
    }

    /**
     * Access Token의 role claim을 추출합니다.
     */
    public String getRole(String token) {
        // role claim을 String 타입으로 읽습니다.
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Access Token인지 확인합니다. Refresh Token이 인증 필터에서 사용되는 것을 막기 위한 방어 로직입니다.
     */
    public boolean isAccessToken(String token) {
        // 인증 필터에서는 API 인증용 Access Token만 허용합니다.
        return "access".equals(parseClaims(token).get("type", String.class));
    }

    /**
     * Refresh Token 만료 시간을 외부에서 응답 DTO와 DB 만료 시간 계산에 재사용합니다.
     */
    public long getRefreshTokenExpiration() {
        // RefreshToken Entity의 expiresAt 계산과 응답 DTO에 재사용합니다.
        return refreshTokenExpiration;
    }

    /**
     * Access Token 만료 시간을 응답 DTO에 넣기 위해 제공합니다.
     */
    public long getAccessTokenExpiration() {
        // TokenResponse에 만료 시간을 알려주기 위해 사용합니다.
        return accessTokenExpiration;
    }

    /**
     * JJWT parser를 통해 claims를 검증 및 파싱합니다.
     */
    private Claims parseClaims(String token) {
        // verifyWith(secretKey)는 서명 검증에 사용할 키를 지정합니다.
        return Jwts.parser()
                .verifyWith(secretKey)
                // parser 설정을 완료합니다.
                .build()
                // 서명된 JWT를 파싱합니다. 실패하면 JwtException이 발생합니다.
                .parseSignedClaims(token)
                // JWT payload(claims)를 꺼냅니다.
                .getPayload();
    }
}
