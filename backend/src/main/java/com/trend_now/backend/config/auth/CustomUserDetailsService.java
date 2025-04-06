package com.trend_now.backend.config.auth;

import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 *  CustomUserDetailsService 클래스의 역할
 *  - Spring Security에서 인증 과정에서 사용되는 사용자 정보(UserDetails 구현체)를 불러오는 클래스
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {
        Members member = memberRepository.findById(Long.parseLong(memberId))
                .orElseThrow(() -> new NotFoundException("MEMBER_ID 일치하는 회원이 없습니다."));
        return new CustomUserDetails(member);
    }
}
