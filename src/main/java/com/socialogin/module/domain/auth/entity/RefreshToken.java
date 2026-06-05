package com.socialogin.module.domain.auth.entity;

import com.socialogin.module.domain.user.entity.User;
import com.socialogin.module.global.entity.GlobalEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Refresh Token 저장 Entity입니다.
 *
 * <p>역할:
 * - Access Token 재발급에 사용할 Refresh Token을 DB에 저장합니다.
 * - 로그아웃, 토큰 탈취 의심, 강제 만료 같은 서버 주도 제어가 가능해집니다.
 *
 * <p>저장 방식:
 * - 예시는 token 원문 저장입니다.
 * - 실무에서는 DB 유출 리스크를 줄이기 위해 token hash 저장을 권장합니다.
 */
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_tokens_token", columnNames = "token")
        },
        indexes = {
                @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
        }
)
public class RefreshToken extends GlobalEntity {
    // RefreshToken 테이블의 DB 기본키입니다.
    @Id
    // MySQL auto increment 전략을 사용합니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Refresh Token이 어느 사용자에게 속하는지 나타내는 연관관계입니다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // user_id FK 컬럼을 만들고 null을 허용하지 않습니다.
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 실제 Refresh Token 문자열입니다. JWT가 길 수 있어 length를 넉넉히 둡니다.
    @Column(nullable = false, length = 1000)
    private String token;

    // 서버가 DB 기준으로도 만료 여부를 판단할 수 있게 만료 시각을 저장합니다.
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Refresh Token 신규 저장 객체를 만듭니다.
     */
    public static RefreshToken create(User user, String token, long refreshTokenExpirationMillis) {
        // Lombok @SuperBuilder가 만들어준 builder로 RefreshToken 객체를 생성합니다.
        return RefreshToken.builder()
                // 토큰 소유자입니다.
                .user(user)
                // 저장할 Refresh Token 문자열입니다.
                .token(token)
                // 현재 시각에 만료 시간(ms)을 더해 expiresAt을 계산합니다.
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMillis)))
                // builder에 넣은 값으로 Entity를 완성합니다.
                .build();
    }

    /**
     * 재발급 요청 시 DB 토큰 만료 여부를 확인하는 책임을 가집니다.
     */
    public boolean isExpired() {
        // 현재 시간이 expiresAt보다 뒤라면 만료된 토큰입니다.
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
