package com.trend_now.backend.common;

import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.domain.Members;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Util {

    private static final String NOT_EXIST_MEMBERS = "회원이 아닙니다.";

    /**
     * 사용자 인증 객체 Members 가 존재하는 지 확인하는 메서드
     */
    public static void checkMemberExist(Members member) {
        // 회원 확인
        if(member == null) {
            throw new NotFoundException(NOT_EXIST_MEMBERS);
        }
    }
}
