package com.socialogin.module.global.security;

import com.socialogin.module.domain.user.entity.UserRole;
import com.socialogin.module.global.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer JWT를 읽어 Spring Security 인증 객체로 변환하는 필터입니다.
 *
 * <p>역할:
 * - 매 요청마다 Access Token을 검증합니다.
 * - 유효한 토큰이면 SecurityContext에 인증 정보를 저장합니다.
 *
 * <p>구조를 이렇게 둔 이유:
 * - JwtTokenProvider는 JWT 생성/검증만 담당하고, SecurityContext 처리는 Filter가 담당합니다.
 * - 책임을 나누면 토큰 정책과 보안 필터 정책을 독립적으로 변경하기 쉽습니다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // JWT는 HTTP Authorization 헤더에 담아 보내는 것이 일반적입니다.
    private static final String AUTHORIZATION_HEADER = "Authorization";

    // Bearer 스킴은 "Authorization: Bearer {token}" 형식에서 사용하는 접두어입니다.
    private static final String BEARER_PREFIX = "Bearer ";

    // 토큰 검증과 claim 추출은 JwtTokenProvider에 위임합니다.
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * HTTP 요청에서 Bearer Token을 추출하고 인증 정보를 SecurityContext에 저장합니다.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 요청 헤더에서 Bearer prefix를 제거한 JWT 문자열을 추출합니다.
        String token = resolveToken(request);

        // 토큰이 있고, 서명/만료가 유효하고, Access Token일 때만 인증 객체를 만듭니다.
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
            // JWT subject에서 사용자 id를 꺼냅니다.
            Long userId = jwtTokenProvider.getUserId(token);

            // JWT role claim에서 사용자 권한을 꺼냅니다.
            String role = jwtTokenProvider.getRole(token);

            // UserRole enum에 권한 문자열 생성 책임을 맡겨 "ROLE_" 규칙 중복을 줄입니다.
            String authority = UserRole.valueOf(role).getAuthority();

            // Spring Security가 이해할 수 있는 Authentication 객체를 생성합니다.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    // principal에는 현재 사용자 식별자인 userId를 넣습니다.
                    userId,
                    // JWT 인증은 비밀번호 credential을 사용하지 않으므로 null입니다.
                    null,
                    // Spring Security가 확인할 권한 목록입니다.
                    List.of(new SimpleGrantedAuthority(authority))
            );

            // IP, session id 같은 웹 요청 세부 정보를 Authentication에 붙입니다.
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // SecurityContext에 인증 객체를 저장하면 이후 Controller/Service에서 인증 사용자로 인식됩니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 인증 성공/실패와 상관없이 다음 필터로 요청을 넘겨야 전체 필터 체인이 계속 진행됩니다.
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization header에서 Bearer prefix를 제거하고 토큰만 반환합니다.
     */
    private String resolveToken(HttpServletRequest request) {
        // Authorization 헤더 값을 읽습니다.
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        // 값이 있고 "Bearer "로 시작하면 JWT 부분만 잘라냅니다.
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // 헤더가 없거나 형식이 다르면 인증 시도 없이 null을 반환합니다.
        return null;
    }
}
