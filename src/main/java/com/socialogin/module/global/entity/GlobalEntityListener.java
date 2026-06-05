package com.socialogin.module.global.entity;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

/**
 * JPA Entity 저장/수정 직전에 공통 시간 필드를 채우는 Listener입니다.
 */
public class GlobalEntityListener {
    /**
     * Entity가 처음 INSERT되기 직전에 호출됩니다.
     */
    @PrePersist
    public void prePersist(GlobalEntity entity) {
        // createdAt과 updatedAt을 같은 시각으로 맞추기 위해 now를 한 번만 구합니다.
        LocalDateTime now = LocalDateTime.now();

        // 새 Entity의 생성 시각을 저장합니다.
        entity.setCreatedAt(now);

        // 새 Entity의 수정 시각도 생성 시각과 동일하게 저장합니다.
        entity.setUpdatedAt(now);
    }

    /**
     * Entity가 UPDATE되기 직전에 호출됩니다.
     */
    @PreUpdate
    public void preUpdate(GlobalEntity entity) {
        // 수정 시각만 현재 시각으로 갱신합니다.
        entity.setUpdatedAt(LocalDateTime.now());
    }
}
