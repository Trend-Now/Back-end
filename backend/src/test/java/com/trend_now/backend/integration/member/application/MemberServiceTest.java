package com.trend_now.backend.integration.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.trend_now.backend.config.auth.JwtTokenFilter;
import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.exception.CustomException.DuplicateException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.yml")
class MemberServiceTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private JwtTokenFilter jwtTokenFilter;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private Members members;

    @BeforeEach
    public void 테스트_전_멤버_객체_DB에_저장() {
        members = Members.builder()
                .name("testUser1")
                .email("testEmail1")
                .snsId("testSnsId1")
                .provider(Provider.TEST)
                .build();
        memberRepository.save(members);
    }

    @Test
    void 닉네임_변경_성공() {
        // given
        String newNickname = "newNickname";

        // when
        memberService.updateNickname(members, newNickname);

        // then
        Members updatedMember = memberRepository.findById(members.getId()).orElseThrow();
        assertEquals(newNickname, updatedMember.getName());
    }

    @Test
    void 닉네임_변경_시_중복된_닉네임이_있으면_에러가_발생한다() {
        // given
        Members testMember = Members.builder()
                .name("testUser2")
                .email("testEmail2")
                .snsId("testSnsId2")
                .provider(Provider.TEST)
                .build();
        memberRepository.save(testMember);

        // when & then
        assertThrows(DuplicateException.class, () -> {
            memberService.updateNickname(testMember, "testUser1");
        });
    }

    @Test
    void 테스트_JWT_값_정상_반환() {
        // given
        // 테스트 JWT 생성 메서드 호출 횟수와 해당 값을 저장할 배열 생성
        int count = 3;
        List<String> jwts = new ArrayList<>();
        Set<Long> memberIds = new HashSet<>();

        // when
        // 테스트 JWT 생성 메서드를 count번 호출한다.
        for (int i = 0; i < count; i++) {
            String jwt = memberService.getTestJwt();
            jwts.add(jwt);
        }

        // then
        // 생성된 JWT 값은 유효해야 한다.
        for (String jwt : jwts) {
            // JWT 토큰이 유효한지 검증
            // jwtTokenFilter.validateToken(jwt) 반환이 null이 아니면 유효한 토큰
            Claims claims = jwtTokenFilter.validateToken(jwt);
            assertThat(claims).isNotNull();

            // Claims에서 사용자 ID 추출 (subject 또는 다른 필드명 사용)
            String userId = claims.getSubject();
            memberIds.add(Long.parseLong(userId));
        }

        // 모든 JWT 토큰이 동일한 테스트 계정의 식별자를 가져야함
        assertThat(memberIds).hasSize(1);

        // 테스트 계정이 DB에 중복 저장되지 않았는지 확인
        List<Members> testMembers = memberRepository.findAllBySnsId("test_snsId");
        assertThat(testMembers).hasSize(1); // 테스트 계정은 하나만 존재해야 함

    }
}