package com.trend_now.backend.config.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
class JwtTokenFilterTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private static final Logger log = LoggerFactory.getLogger(JwtTokenFilterTest.class);

    private Members testMember;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private int expiration;

    private static final String NOT_EXIST_MEMBER = "MEMBER_ID 일치하는 회원이 없습니다.";

    @BeforeEach
    public void setUp() {
        testMember = memberRepository.save(Members.builder()
                .name("testUser")
                .email("testEmail")
                .snsId("testSnsId")
                .provider(Provider.TEST)
                .build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("정상적인 로그인인 경우, 권한이 ROLE_USER인 Authentication 객체가 만들어저야 한다.")
    public void 사용자_정보_조회_성공() {
        // given
        // 정상 로그인으로 CustomUserDetails 객체 생성
        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService
                .loadUserByUsername(String.valueOf(testMember.getId()));

        // when
        // 인증 객체 생성됐을 때,
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // then
        // 인증 객체의 권한은 "ROLE_USER"이어야 한다.
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER");
        assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        assertThat(((CustomUserDetails) authentication.getPrincipal()).getUsername())
                .isEqualTo(String.valueOf(testMember.getId()));
    }

    @Test
    @DisplayName("비정상적인 로그인인 경우 Authentication 객체가 만들어지면 안된다.")
    public void 비정상_로그인_경우_예외처리() {
        // given
        // 비정상 MemberId
        String invalidMemberId = "-1";

        // when & then
        // 비정상 MemberId로 CustomUserDetails를 생성할 때, NotFoundException이 발생해야 한다.
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(invalidMemberId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(NOT_EXIST_MEMBER);
    }

    @Test
    @DisplayName("만료된 JWT는 JWTTokenFilter에서 예외 처리되어야 한다.")
    public void 만료된_JWT() {
        // given
        // 만료시간이 지난 JWT
        Date now = new Date();
        Key SECRET_KEY = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKey),
                SignatureAlgorithm.HS512.getJcaName());
        String expiredToken = Jwts.builder()
                .setClaims(Jwts.claims().setSubject(String.valueOf(testMember.getId())))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() - expiration * 60 * 1000L))
                .signWith(SECRET_KEY)
                .compact();

        // when & then
        // validateToken() 메서드에 의해서 검증 안된 JWT는 null이 반환
        assertThat(jwtTokenFilter.validateToken(expiredToken)).isNull();
    }
}