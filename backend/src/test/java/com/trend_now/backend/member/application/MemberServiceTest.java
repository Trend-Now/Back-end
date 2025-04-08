package com.trend_now.backend.member.application;

import static org.junit.jupiter.api.Assertions.*;

import com.trend_now.backend.exception.CustomException.DuplicateException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.yml")
class MemberServiceTest {

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

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
}