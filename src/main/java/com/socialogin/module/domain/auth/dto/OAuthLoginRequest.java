package com.socialogin.module.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프론트엔드 callback 페이지가 OAuth provider authorization code를 백엔드 로그인 API로 전달할 때 쓰는 요청 DTO입니다.
 */
@Schema(description = "소셜 로그인 code 전달 요청")
public record OAuthLoginRequest(
        // provider가 redirect_uri로 돌려준 authorization code입니다.
        @Schema(description = "OAuth provider가 발급한 authorization code", example = "AQAB...", requiredMode = Schema.RequiredMode.REQUIRED)
        String code
) {
}
