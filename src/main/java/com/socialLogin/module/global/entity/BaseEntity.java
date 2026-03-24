package com.socialLogin.module.global.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

/**
 * BaseEntity 사용 이유 : Entity 클래스에서 공통으로 사용되는 생성일, 수정일, 생성자, 수정자 등 필드를 정의
 * 중복 제거 일관 방식으로 관리함
 * 추상 클래스로 설정 -> BaseEntity 자체를 직접 객체로 생성하지 못하게 막음
 * 상속 구조를 사용해서 공통 기능을 부모 클래스 구현하고 상속 통해 확장성 제공
 */

@Getter // private 필드를 외부에서 안전하게 읽을수 있도록 사용
@MappedSuperclass // 공통 필드를 부모 클래스로 빼버림 -> 일반 @Entity 사용하면 DB에 부모 테이블이 따로 생기지만 @MappedSuperclass 사용시 부모 테이블 없이 자식 테이블에 컬럼만 추가됨
@EntityListeners(AuditingEntityListener.class) // 생성 시간 수정 시간 등 지정할 때 LocalDateTime.now() 메서드를 사용하면 직접 넣어야 해서 불편함 -> @EntityListeners(AuditingEntityListener.class(시간 자동입력 감시자 역할))는 자동으로 채워줌
public abstract class  BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Setter(PROTECTED) // 외부에서 마음대로 id값을 바꾸지 못하게 설정
    private Long id; // 같은 패키지 자식 클래스에서는 접근 가능함

    @CreatedDate
    private LocalDateTime createdAt; // 생성일 (엔티티 저장 시 자동 입력)

    @LastModifiedDate
    private LocalDateTime updatedAt; // 수정일 (엔티티 수정 시 자동 입력)
}