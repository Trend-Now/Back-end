package com.trend_now.backend.member.application;

import com.trend_now.backend.common.CookieUtil;
import com.trend_now.backend.config.auth.JwtTokenFilter;
import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.config.auth.oauth.OAuthAttributes;
import com.trend_now.backend.exception.customException.DuplicateException;
import com.trend_now.backend.exception.customException.InvalidTokenException;
import com.trend_now.backend.exception.customException.NotFoundException;
import com.trend_now.backend.member.data.dto.MyPageResponseDto;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.post.repository.ScrapRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private static final String NOT_EXIST_MEMBER = "존재하지 않는 회원입니다.";
    private static final String NOT_EXIST_MATCHED_REFRESH_TOKEN_IN_REDIS = "Redis에 일치하는 Refresh Token이 존재하지 않습니다.";
    private static final String DUPLICATE_NICKNAME = "이미 존재하는 닉네임입니다.";
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    private static final String REISSUANCE_ACCESS_TOKEN_SUCCESS = "Access Token 재발급에 성공하였습니다.";
    private static final String REISSUANCE_ACCESS_TOKEN_FAIL = "Access Token 재발급에 실패하였습니다.";
    private static final String NOT_EXIST_ACCESS_TOKEN = "Access Token이 존재하지 않습니다.";
    private static final String NOT_EXIST_REFRESH_TOKEN = "Refresh Token이 존재하지 않습니다.";
    private static final int ONE_YEAR = 365*24*60*60;   // 1년을 초로 변환
    private static final String LOGOUT_SUCCESS = "로그아웃에 성공하였습니다.";
    private static final String LOGOUT_FAIL = "로그아웃에 실패하였습니다.";

    private final MemberRepository memberRepository;
    private final PostsRepository postsRepository;
    private final ScrapRepository scrapRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRedisService memberRedisService;
    private final JwtTokenFilter jwtTokenFilter;

//    @Value("${jwt.access-token.expiration}")
//    private int accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private int refreshTokenExpiration;

    /**
     * 마이페이지 조회
     */
    public MyPageResponseDto getMyPage(Long memberId) {
        Members members = memberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_MEMBER));
        return MyPageResponseDto.of(members.getName(), members.getEmail());
    }

    /**
     * 임의 닉네임 생성 메서드 - name은 중복 허용 컬럼이므로 DB 조회없이 자바 에플리케이션 레벨에서 임의 생성 - 임의 알파벳 소문자 4개 + 임의 숫자 4개로
     * 구성
     */
    public static String createNickname() {
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
        postsRepository.deleteAllByMembers_Id(memberId);
        scrapRepository.deleteAllByMembers_Id(memberId);
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

    // OAuth로 가져온 정보를 통해 회원 정보를 저장
    @Transactional
    public Members saveOrUpdate(OAuthAttributes attributes) {
        return memberRepository.findBySnsIdAndProvider(attributes.getSnsId(), attributes.getProvider())
            .orElseGet(() -> memberRepository.save(attributes.toEntity()));
    }

    /**
     * test jwt 값을 얻기 위해서 test 계정을 이용하여 JWT 값 생성 및 반환
     * - test 계정이 있으면 해당 계정의 id 값을 가져와 JWT 값 생성
     * - test 계정이 없으면 해당 계정을 저장 후, id 값을 가져와 JWT 값 생성
     */
    @Transactional
    public String getTestJwt(HttpServletRequest request, HttpServletResponse response) {
        Members testMember = memberRepository.findBySnsId("test_snsId")
                .orElseGet(
                        () -> memberRepository.save(
                                Members.builder()
                                        .name("test_name")
                                        .email("test_email")
                                        .provider(Provider.TEST)
                                        .snsId("test_snsId")
                                        .build()
                        )
                );

        String testJwt = jwtTokenProvider.createAccessToken(testMember.getId());
        String testRefreshToken = jwtTokenProvider.createRefreshToken(testMember.getId());
        log.info("[MemberService.getTestJwt] : 테스트용 JWT = {}, Refresh Token {}", testJwt, testRefreshToken);
        CookieUtil.addCookie(request, response, ACCESS_TOKEN_KEY, testJwt, ONE_YEAR);
        CookieUtil.addCookie(request, response, REFRESH_TOKEN_KEY, testRefreshToken, refreshTokenExpiration);
        return testJwt;
    }

    /**
     * Access Token 재발급
     * - 만료된 Access Token에서 Member Id 추출
     * - Redis에 key(Member Id)에 대한 Refresh Token과 Request의 Refresh Token이 일치하면 Access Token 생성하여 Cookie 저장
     */
    public String reissuanceAccessToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie expiredAccessToken = CookieUtil.getCookie(request, ACCESS_TOKEN_KEY).orElseThrow(
                () -> new InvalidTokenException(NOT_EXIST_ACCESS_TOKEN)
        );

        Cookie refreshToken = CookieUtil.getCookie(request, REFRESH_TOKEN_KEY).orElseThrow(
                () -> new InvalidTokenException(NOT_EXIST_REFRESH_TOKEN)
        );

        Long memberIdInAccessToken = jwtTokenFilter.extractMemberId(expiredAccessToken.getValue());

        log.info("[MemberService.reissuanceAccessToken] : 추출된 Access Token = {}, Refresh Token = {}, Member Id = {}"
                , expiredAccessToken.getValue(), refreshToken.getValue(), memberIdInAccessToken);

        // Redis에 key(Member Id)의 value(Refresh Token)이 입력된 Refresh Token과 일치하는지 확인
        if(memberRedisService.isMatchedRefreshTokenInRedis(memberIdInAccessToken, refreshToken.getValue())) {
            String reissuancedAccessToken = jwtTokenProvider.createAccessToken(memberIdInAccessToken);
            CookieUtil.addCookie(request, response, ACCESS_TOKEN_KEY, reissuancedAccessToken, ONE_YEAR);
            return REISSUANCE_ACCESS_TOKEN_SUCCESS;
        } else {
            throw new InvalidTokenException(NOT_EXIST_MATCHED_REFRESH_TOKEN_IN_REDIS);
        }
    }

    /**
     * 로그아웃
     * - Redis의 Refresh Token 부분을 삭제
     * - Cookie 측에 Access Token과 Refresh Token 부분을 삭제
     */
    public String logout(HttpServletRequest request, HttpServletResponse response, Members member) {
        try {
            // Redis에 key(Member Id)의 value(Refresh Token) 부분을 삭제
            memberRedisService.delete(String.valueOf(member.getId()));

            // Cookie 측에 Access Token과 Refresh Token 삭제
            CookieUtil.addCookie(request, response, ACCESS_TOKEN_KEY, null, 0);
            CookieUtil.addCookie(request, response, REFRESH_TOKEN_KEY, null, 0);

            return LOGOUT_SUCCESS;
        } catch (Exception e) {
            log.error("[MemberService.logout] 로그아웃 에러 = {}", e.getMessage());
            return LOGOUT_FAIL;
        }
    }
}
