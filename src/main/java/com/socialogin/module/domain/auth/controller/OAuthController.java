package com.socialogin.module.domain.auth.controller;

import com.socialogin.module.domain.auth.dto.OAuthLoginRequest;
import com.socialogin.module.domain.auth.dto.OAuthLoginUrlResponse;
import com.socialogin.module.domain.auth.dto.TokenResponse;
import com.socialogin.module.domain.auth.service.OAuthService;
import com.socialogin.module.global.oauth.OAuthProvider;
import com.socialogin.module.global.rsdata.RsData;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth 소셜 로그인 HTTP API를 제공하는 Controller입니다.
 *
 * <p>역할:
 * - provider authorization URL 생성 또는 302 redirect를 처리합니다.
 * - 프론트엔드 callback 페이지가 전달한 code로 로그인 API를 호출하게 합니다.
 * - callback URL에서 우리 서비스 JWT가 직접 노출되지 않도록 callback endpoint는 토큰을 발급하지 않습니다.
 */
@Tag(name = "OAuth 로그인", description = "Google, Kakao, Naver, Facebook, GitHub 소셜 로그인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/oauth")
public class OAuthController {
    // HTTP 요청/응답만 담당하고 실제 OAuth 흐름은 OAuthService에 위임합니다.
    private final OAuthService oAuthService;

    /**
     * provider 로그인 URL을 JSON으로 반환합니다.
     */
    @Operation(
            summary = "소셜 로그인 URL 조회",
            description = "프론트엔드가 직접 window.location을 변경할 때 사용할 provider authorization URL을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 URL 생성 성공",
                    content = @Content(
                            schema = @Schema(implementation = RsData.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "provider": "naver",
                                        "loginUrl": "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=...",
                                        "state": "550e8400-e29b-41d4-a716-446655440000"
                                      },
                                      "error": null,
                                      "timestamp": "2026-05-20T12:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 provider"),
            @ApiResponse(responseCode = "500", description = "OAuth provider 설정 누락")
    })
    @GetMapping("/{provider}/login-url")
    public RsData<OAuthLoginUrlResponse> getLoginUrl(
            @Parameter(
                    description = "소셜 로그인 제공자",
                    example = "naver",
                    schema = @Schema(allowableValues = {"google", "kakao", "naver", "facebook", "github"})
            )
            @PathVariable String provider,
            @Parameter(description = "OAuth state 값입니다. 비워두면 서버가 UUID를 생성합니다.", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) String state
    ) {
        // 프론트엔드가 provider URL과 state를 직접 관리할 수 있도록 DTO를 공통 응답으로 감싸 반환합니다.
        return RsData.success(oAuthService.getLoginUrl(provider, state));
    }

    /**
     * provider 로그인 페이지로 바로 redirect합니다.
     */
    @Operation(
            summary = "소셜 로그인 시작",
            description = "브라우저를 provider authorization URL로 302 redirect합니다. JWT는 이 단계에서 발급하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "provider 로그인 페이지로 redirect",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = HttpHeaders.LOCATION,
                            description = "provider authorization URL"
                    )
            ),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 provider"),
            @ApiResponse(responseCode = "500", description = "OAuth provider 설정 누락")
    })
    @GetMapping("/{provider}/authorize")
    public ResponseEntity<Void> authorize(
            @Parameter(
                    description = "소셜 로그인 제공자",
                    example = "naver",
                    schema = @Schema(allowableValues = {"google", "kakao", "naver", "facebook", "github"})
            )
            @PathVariable String provider,
            @Parameter(description = "OAuth state 값입니다. 비워두면 서버가 UUID를 생성합니다.", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) String state
    ) {
        // provider authorization URL을 만들고 브라우저를 그 주소로 이동시킵니다.
        OAuthLoginUrlResponse response = oAuthService.getLoginUrl(provider, state);
        return redirect(response.loginUrl());
    }

    /**
     * 기존 GET /login 사용자를 위한 호환 endpoint입니다.
     *
     * <p>새 코드에서는 GET /authorize 사용을 권장합니다.
     */
    @Deprecated
    @Hidden
    @GetMapping("/{provider}/login")
    public ResponseEntity<Void> redirectToLogin(
            @PathVariable String provider,
            @RequestParam(required = false) String state
    ) {
        // 과거 GET /login 흐름도 JWT를 발급하지 않고 provider 페이지로 이동만 시킵니다.
        return authorize(provider, state);
    }

    /**
     * 프론트엔드 callback 페이지가 전달한 authorization code로 로그인하고 JWT를 발급합니다.
     */
    @Operation(
            summary = "소셜 로그인 처리",
            description = "프론트엔드가 callback URL의 code를 추출해 전달하면 provider token 교환, 사용자 조회, 회원가입/로그인, JWT 발급을 수행합니다. state는 프론트엔드 callback에서 검증하고 백엔드 body에는 포함하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "소셜 로그인 성공",
                    content = @Content(
                            schema = @Schema(implementation = RsData.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "tokenType": "Bearer",
                                        "accessTokenExpiresIn": 3600000,
                                        "refreshTokenExpiresIn": 604800000
                                      },
                                      "error": null,
                                      "timestamp": "2026-05-20T12:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "authorization code 누락, 만료, 재사용 또는 redirect_uri 불일치",
                    content = @Content(
                            schema = @Schema(implementation = RsData.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": "INVALID_AUTHORIZATION_CODE",
                                        "message": "facebook 인가코드 교환에 실패했습니다. 인가코드가 만료되었거나 이미 사용되었거나 authorize 요청의 redirect_uri와 token 요청의 redirect_uri가 다를 수 있습니다."
                                      },
                                      "timestamp": "2026-05-20T12:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "provider client 인증 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 다른 provider로 가입된 이메일",
                    content = @Content(
                            schema = @Schema(implementation = RsData.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": {
                                        "code": "DUPLICATED_EMAIL_WITH_DIFFERENT_PROVIDER",
                                        "message": "이미 NAVER 로그인으로 가입된 이메일입니다. NAVER 로그인을 이용해주세요."
                                      },
                                      "timestamp": "2026-05-20T12:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "provider token/user-info API 연동 실패",
                    content = @Content(schema = @Schema(implementation = RsData.class))
            )
    })
    @PostMapping("/{provider}/login")
    public RsData<TokenResponse> login(
            @Parameter(
                    description = "소셜 로그인 제공자",
                    example = "naver",
                    schema = @Schema(allowableValues = {"google", "kakao", "naver", "facebook", "github"})
            )
            @PathVariable String provider,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "프론트엔드 callback 페이지가 URL query parameter에서 추출한 authorization code",
                    content = @Content(
                            schema = @Schema(implementation = OAuthLoginRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "AQAB..."
                                    }
                                    """)
                    )
            )
            @RequestBody OAuthLoginRequest request
    ) {
        // JWT 발급은 이 POST login API에서만 수행하고, 결과를 공통 응답으로 감싸 반환합니다.
        return RsData.success(oAuthService.login(provider, request));
    }

    /**
     * provider가 redirect_uri로 호출하는 backend callback입니다.
     *
     * <p>이 callback은 JWT를 발급하지 않고 빈 본문의 200 응답만 반환합니다.
     * authorization code는 callback URL의 query(?code=...)에 담겨 있으며,
     * 클라이언트는 그 code를 POST /{provider}/login으로 보내 JWT를 발급받습니다(2단계 구조).
     *
     * <p>204가 아니라 200을 쓰는 이유: 브라우저는 최상위 navigation 중 204를 받으면
     * 이전 페이지(provider 동의 화면)에 그대로 머물러 callback URL로 이동하지 않습니다.
     * 200 빈 본문으로 응답해야 브라우저가 callback URL로 이동해 주소창의 code를 볼 수 있습니다.
     *
     * <p>callback URL에 JWT가 직접 노출되지 않도록, 토큰 발급은 POST /login에서만 수행합니다.
     */
    @Operation(
            summary = "소셜 로그인 callback (빈 응답)",
            description = "JWT를 발급하지 않고 빈 본문의 200 응답만 반환합니다. authorization code는 callback URL의 query에 있으며, 그 code를 POST /{provider}/login으로 보내야 JWT가 발급됩니다."
    )
    @Hidden
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT를 발급하지 않는 빈 callback 응답(코드는 URL query에 있음)"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 provider")
    })
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(
            @Parameter(
                    description = "소셜 로그인 제공자",
                    example = "naver",
                    schema = @Schema(allowableValues = {"google", "kakao", "naver", "facebook", "github"})
            )
            @PathVariable String provider,
            @Parameter(description = "OAuth provider가 발급한 authorization code")
            @RequestParam(required = false) String code,
            @Parameter(description = "로그인 URL 생성 시 사용한 OAuth state 값")
            @RequestParam(required = false) String state
    ) {
        // provider 값만 검증하고 code/state로 토큰 교환을 하지 않습니다.
        // 이 endpoint는 callback URL에서 JWT JSON이 노출되는 문제를 막기 위한 빈 응답입니다.
        // 단, 204가 아니라 200을 반환해 브라우저가 callback URL로 실제 이동(주소창에 code 노출)하도록 합니다.
        OAuthProvider.fromOAuthProvider(provider);

        return ResponseEntity.ok().build();
    }

    /**
     * 302 redirect 응답을 생성합니다.
     */
    private ResponseEntity<Void> redirect(String location) {
        // Location 헤더를 받은 브라우저가 provider authorization URL로 이동합니다.
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }
}
