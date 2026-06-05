package com.socialogin.module.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 모든 Entity가 공통으로 상속받는 시간/삭제 상태 필드입니다.
 *
 * <p>역할:
 * - createdAt, updatedAt, deletedAt을 Entity마다 반복 선언하지 않게 합니다.
 * - GlobalEntityListener와 함께 생성/수정 시각을 자동으로 채웁니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
@EntityListeners(GlobalEntityListener.class)
public class GlobalEntity {

    // Entity가 처음 저장된 시각입니다. GlobalEntityListener.prePersist에서 자동 세팅됩니다.
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Entity가 마지막으로 수정된 시각입니다. 저장/수정 시 Listener가 자동 세팅합니다.
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // null이면 삭제되지 않은 데이터, 값이 있으면 소프트 삭제된 데이터로 봅니다.
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 실제 DB row를 지우지 않고 deletedAt만 채워 삭제 상태로 표시합니다.
     */
    public void softDelete() {
        // 현재 시각을 deletedAt에 저장해 "이 시점에 삭제 처리됨"을 표현합니다.
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 소프트 삭제 여부를 확인합니다.
     */
    public boolean isDeleted() {
        // deletedAt이 null이 아니면 삭제 처리된 데이터입니다.
        return this.deletedAt != null;
    }

    /**
     * 기존 오타 메서드를 호출하는 코드가 있어도 깨지지 않도록 남겨둔 호환용 메서드입니다.
     */
    @Deprecated
    public boolean isDealeted() {
        // 올바른 이름의 isDeleted로 위임합니다.
        return isDeleted();
    }
}
