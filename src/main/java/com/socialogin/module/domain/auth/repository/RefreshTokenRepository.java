package com.socialogin.module.domain.auth.repository;

import com.socialogin.module.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Refresh Token 영속성을 담당하는 Repository입니다.
 *
 * <p>역할:
 * - Refresh Token 조회, 사용자별 기존 토큰 삭제를 담당합니다.
 * - OAuthService가 JPA 구현 세부사항을 알지 않게 합니다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    /**
     * 토큰 재발급 API를 만들 때 Refresh Token 원문 또는 hash로 조회합니다.
     *
     * <p>Spring Data JPA가 메서드 이름을 기반으로 token 컬럼 조회 쿼리를 자동 생성합니다.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자별 Refresh Token을 하나만 유지하고 싶을 때 기존 토큰을 제거합니다.
     *
     * <p>OAuthService.saveRefreshToken에서 새 토큰 저장 직전에 호출합니다.
     */
    void deleteByUserId(Long userId);
}
