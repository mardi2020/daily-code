package org.mardi2020.transactional.config;

public enum Propagation {
    REQUIRED,          // 기존 트랜잭션 있으면 참여, 없으면 새로 시작
    REQUIRES_NEW,      // 항상 새로운 트랜잭션 시작
    SUPPORTS,          // 트랜잭션이 있으면 참여, 없으면 비트랜잭션으로 실행
    MANDATORY,         // 반드시 기존 트랜잭션이 있어야 함, 없으면 예외 발생
    NEVER,             // 트랜잭션이 있으면 예외 발생
    NOT_SUPPORTED      // 트랜잭션 없이 실행
}
