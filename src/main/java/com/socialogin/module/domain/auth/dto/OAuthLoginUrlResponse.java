package com.socialogin.module.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 소셜 로그인 페이지 URL을 클라이언트에 반환하는 DTO입니다.
 */
@Schema(description = "소셜 로그인 URL 응답")
public record OAuthLoginUrlResponse(
        // 요청한 provider 이름입니다. 프론트엔드 디버깅/분기에 사용할 수 있습니다.
        @Schema(description = "소셜 로그인 제공자", example = "google")
        String provider,

        // 브라우저를 이동시킬 provider authorization URL입니다.
        @Schema(description = "provider 로그인 페이지 URL", example = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=...")
        String loginUrl,

        // callback에서 같은 요청의 응답인지 확인할 때 사용할 state 값입니다.
        @Schema(description = "OAuth state 값. CSRF 방지와 callback 검증에 사용합니다.", example = "550e8400-e29b-41d4-a716-446655440000")
        String state
) {
}
