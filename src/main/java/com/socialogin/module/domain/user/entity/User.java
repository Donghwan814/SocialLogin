package com.socialogin.module.domain.user.entity;

import com.socialogin.module.global.entity.GlobalEntity;
import com.socialogin.module.global.oauth.OAuthProvider;
import com.socialogin.module.global.oauth.userinfo.OAuthUserInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 우리 서비스의 사용자 Entity입니다.
 *
 * <p>역할:
 * - 소셜 provider의 사용자를 우리 서비스 회원으로 매핑합니다.
 * - provider + providerId를 고유 식별자로 사용해 같은 소셜 계정 중복 가입을 막습니다.
 * - email은 전체 회원에서 고유하게 관리해 서로 다른 로그인 방식의 자동 병합을 막습니다.
 *
 * <p>OOP 관점:
 * - User는 자신의 생성 규칙을 createOAuthUser 정적 팩토리에 가집니다.
 * - Service가 Entity 내부 필드 세팅 규칙을 자세히 알 필요 없도록 합니다.
 */
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = {"provider", "provider_id"}),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_users_provider_provider_id", columnList = "provider, provider_id")
        }
)
public class User extends GlobalEntity {
    // DB 기본키입니다. 내부 식별자로 사용합니다.
    @Id
    // MySQL auto increment 전략을 사용합니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소셜 provider에서 받은 이메일입니다. provider가 이메일을 주지 않을 수 있어 nullable이며, 신원 기준은 provider+providerId입니다.
    // unique 제약은 유지되지만 MySQL은 NULL을 여러 개 허용하므로 이메일이 있을 때만 고유성이 적용됩니다.
    @Column(length = 255)
    private String email;

    // 로컬 로그인 회원의 암호화된 비밀번호입니다. 소셜 로그인 회원은 비밀번호가 없으므로 null을 허용합니다.
    @Column(length = 255)
    private String password;

    // 화면 표시용 이름입니다. provider nickname/name이 없으면 fallback 값을 씁니다.
    @Column(nullable = false, length = 100)
    private String name;

    // 어떤 provider로 가입했는지 저장합니다.
    @Enumerated(EnumType.STRING)
    // enum을 숫자가 아니라 문자열로 저장해 DB 가독성과 안전성을 높입니다.
    @Column(nullable = false, length = 30)
    private OAuthProvider provider;

    // provider 안에서 사용자를 식별하는 고유 ID입니다.
    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    // 서비스 권한입니다. 지금은 USER 기본 권한을 부여합니다.
    @Enumerated(EnumType.STRING)
    // enum ordinal 대신 문자열로 저장해 enum 순서 변경에 안전하게 만듭니다.
    @Column(nullable = false, length = 30)
    private UserRole role;

    /**
     * OAuthUserInfo를 기반으로 신규 소셜 회원을 생성합니다.
     */
    public static User createOAuthUser(OAuthUserInfo userInfo) {
        // Lombok @SuperBuilder가 만들어준 builder로 User 객체를 생성합니다.
        return User.builder()
                // provider에서 받은 이메일을 저장합니다.
                .email(userInfo.email())
                // 소셜 회원은 이메일/비밀번호 인증에 사용할 password가 없으므로 null로 둡니다.
                .password(null)
                // name이 없을 수도 있으므로 resolveName으로 안전하게 결정합니다.
                .name(resolveName(userInfo))
                // 어떤 provider인지 저장합니다.
                .provider(userInfo.provider())
                // provider의 사용자 고유 ID를 저장합니다.
                .providerId(userInfo.providerId())
                // 신규 가입자는 기본 USER 권한을 가집니다.
                .role(UserRole.USER)
                // builder에 넣은 값으로 User 객체를 완성합니다.
                .build();
    }

    /**
     * provider가 이름을 주지 않는 경우에도 DB not-null 제약을 만족하도록 기본 이름을 만듭니다.
     */
    private static String resolveName(OAuthUserInfo userInfo) {
        // provider가 nickname/name을 내려줬고 빈 문자열이 아니라면 그 값을 그대로 사용합니다.
        if (userInfo.nickname() != null && !userInfo.nickname().isBlank()) {
            return userInfo.nickname();
        }

        // 이름이 없으면 provider_providerId 형태의 안전한 기본 이름을 만듭니다.
        return userInfo.provider().getRegistrationId() + "_" + userInfo.providerId();
    }
}
