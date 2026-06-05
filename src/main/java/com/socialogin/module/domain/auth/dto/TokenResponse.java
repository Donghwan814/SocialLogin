package com.socialogin.module.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 자체 JWT 발급 결과를 클라이언트에 반환하는 DTO입니다.
 */
@Schema(description = "JWT 토큰 응답")
public record TokenResponse(
        // 우리 서비스 API 인증에 사용하는 짧은 수명의 JWT입니다.
        @Schema(description = "API 인증에 사용할 Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        // Access Token 재발급에 사용할 긴 수명의 JWT입니다.
        @Schema(description = "Access Token 재발급에 사용할 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        // Authorization 헤더에 붙일 토큰 타입입니다. 일반적으로 Bearer입니다.
        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        // 프론트엔드가 Access Token 갱신 타이밍을 계산할 수 있게 만료 시간을 내려줍니다.
        @Schema(description = "Access Token 만료 시간. 단위는 밀리초입니다.", example = "3600000")
        long accessTokenExpiresIn,

        // Refresh Token 만료 시간입니다.
        @Schema(description = "Refresh Token 만료 시간. 단위는 밀리초입니다.", example = "604800000")
        long refreshTokenExpiresIn
) {
    public static TokenResponse bearer(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn,
            long refreshTokenExpiresIn
    ) {
        // OAuthService가 tokenType을 매번 직접 넣지 않도록 Bearer 응답 생성 규칙을 한 곳에 모읍니다.
        return new TokenResponse(accessToken, refreshToken, "Bearer", accessTokenExpiresIn, refreshTokenExpiresIn);
    }
}
