package com.socialogin.module.domain.user.repository;

import com.socialogin.module.domain.user.entity.User;
import com.socialogin.module.global.oauth.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User Entity 영속성을 담당하는 Repository입니다.
 *
 * <p>역할:
 * - OAuth 로그인 시 provider + providerId로 기존 회원을 찾습니다.
 * - 이메일 충돌 여부를 확인해 다른 provider 계정 탈취 가능성을 줄입니다.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 소셜 provider의 고유 사용자 id로 우리 회원을 조회합니다.
     *
     * <p>Spring Data JPA가 메서드 이름을 해석해
     * "where provider = ? and providerId = ?" 쿼리를 자동 생성합니다.
     */
    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    /**
     * 동일 이메일이 이미 다른 provider로 가입되어 있는지 확인할 때 사용합니다.
     *
     * <p>이 검사는 서로 다른 소셜 계정을 같은 이메일만 보고 자동 병합하지 않기 위한 안전장치입니다.
     */
    Optional<User> findByEmail(String email);
}
