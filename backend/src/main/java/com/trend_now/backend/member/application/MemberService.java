package com.trend_now.backend.member.application;

import com.trend_now.backend.exception.CustomException.DuplicateException;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.data.vo.GoogleProfile;
import com.trend_now.backend.member.data.vo.KakaoProfile;
import com.trend_now.backend.member.data.vo.NaverProfile;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private static final String NOT_EXIST_MEMBER = "존재하지 않는 회원입니다.";
    private static final String DUPLICATE_NICKNAME = "이미 존재하는 닉네임입니다.";

    private final MemberRepository memberRepository;
    private final PostsRepository postsRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Members createGoogleOauth(GoogleProfile googleProfile, Provider provider) {
        Members member = Members.builder()
            .email(googleProfile.getEmail())
            .name(createNickname())
            .provider(provider)
            .snsId(googleProfile.getSub())
            .build();

        memberRepository.save(member);
        return member;
    }

    @Transactional
    public Members createKakaoOauth(KakaoProfile kakaoProfile, Provider provider) {
        Members member = Members.builder()
            .email(kakaoProfile.getKakao_account().getEmail())
            .name(createNickname())
            .provider(provider)
            .snsId(kakaoProfile.getId())
            .build();

        memberRepository.save(member);
        return member;
    }

    @Transactional
    public Members createNaverOauth(NaverProfile naverProfile, Provider provider) {
        Members member = Members.builder()
            .email(naverProfile.getResponse().getEmail())
            .name(createNickname())
            .provider(provider)
            .snsId(naverProfile.getResponse().getId())
            .build();

        memberRepository.save(member);
        return member;
    }

    /**
     * 임의 닉네임 생성 메서드 - name은 중복 허용 컬럼이므로 DB 조회없이 자바 에플리케이션 레벨에서 임의 생성 - 임의 알파벳 소문자 4개 + 임의 숫자 4개로
     * 구성
     */
    private String createNickname() {
        StringBuilder nickname = new StringBuilder();
        Random random = new Random();

        // 영어 4글자 (소문자)
        for (int i = 0; i < 4; i++) {
            char letter = (char) ('a' + random.nextInt(26)); // 'a' ~ 'z' 랜덤 문자
            nickname.append(letter);
        }

        // 숫자 4자리 (0000 ~ 9999)
        int number = random.nextInt(10000);
        nickname.append(String.format("%04d", number));

        return nickname.toString();
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteMember(Long memberId) {
        memberRepository.deleteById(memberId);
        log.info("회원 탈퇴 완료 - {}", memberId);
    }

    @Transactional
    public void updateNickname(Members member, String nickname) {
        if (memberRepository.existsByName(nickname)) {
            throw new DuplicateException(DUPLICATE_NICKNAME);
        }
        // 영속 상태로 가져오기 위해 다시 조회
        Members members = memberRepository.findById(member.getId())
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_MEMBER));
        members.setName(nickname);
        log.info("닉네임 변경 완료 - {}", member.getName());
    }
}
