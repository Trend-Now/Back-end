package com.trend_now.backend.member.application;

import com.trend_now.backend.member.data.vo.GoogleProfile;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Members createOauth(GoogleProfile googleProfile, Provider provider) {
        Members member = Members.builder()
                .email(googleProfile.getEmail())
                .name("test")
                .provider(provider)
                .snsId(googleProfile.getSub())
                .build();

        memberRepository.save(member);
        return member;
    }
}
