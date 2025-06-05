package com.trend_now.backend.member.domain;

/**
 *  권한, 역할의 Enum 클래스로 추후 확장성을 위해 생성
 */
public enum Role {

    ADMIN, USER;

    private static final String ROLE_PREFIX = "ROLE_";

    public String getAuthority() {
        return ROLE_PREFIX + this.name();
    }

}
