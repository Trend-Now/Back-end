package com.trend_now.backend.member.application;

import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.member.data.vo.AccessToken;
import com.trend_now.backend.member.data.vo.AuthCodeToJwtRequest;
import com.trend_now.backend.member.data.vo.NaverProfile;
import com.trend_now.backend.member.data.vo.OAuth2LoginResponse;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class NaverService {

    @Value("${oauth.naver.client-id}")
    private String naverClientId;

    @Value("${oauth.naver.client-secret}")
    private String naverClientSecret;

    @Value("${oauth.naver.redirect-uri}")
    private String naverRedirectUri;

    private static final String CODE = "code";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String GRANT_TYPE = "grant_type";
    private static final String AUTHORIZATION_CODE = "authorization_code";

    // state는 네이버 측에서 csrf 방지를 위한 임시 값
    // todo. 프론트엔드 측에서 요청할 때도, state가 쓰이므로 합의 진행 필요
    private static final String STATE = "state";
    private static final String STATE_VALUE = "test";

    private static final String ACCESS_TOKEN_NAVER_URI = "https://nid.naver.com/oauth2.0/token";
    private static final String ACCESS_TOKEN_HEADER_NAME = "Content-Type";
    private static final String ACCESS_TOKEN_HEADER_VALUE = "application/x-www-form-urlencoded";

    private static final String PROFILE_NAVER_URI = "https://openapi.naver.com/v1/nid/me";
    private static final String PROFILE_HEADER_NAME = "Authorization";
    private static final String PROFILE_HEADER_VALUE = "Bearer ";

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     *  getToken(AuthCodeToJwtRequest)
     *  - 인가 코드를 통해 JWT 토큰을 반환받는다.
     *  - 이 과정에는 인가 코드를 통해 Access Token을 발급 받고, Access Token을 통해 네이버로부터 사용자 정보를 받는다.
     *  - 사용자 정보를 본 서비스에서 검증하여 JWT 토큰을 발급해준다.
     */
    public OAuth2LoginResponse getToken(AuthCodeToJwtRequest authCodeToJwtRequest) {
        AccessToken accessToken = getAccessToken(authCodeToJwtRequest.getCode());
        NaverProfile naverProfile = getNaverProfile(accessToken.getAccess_token());

        // socialId를 통해 회원 탐색(없으면 null 반환)
        // 최초 로그인 경우, 회원 정보 저장
        Members originalMember = memberRepository.findBySnsId(naverProfile.getResponse().getId())
                .orElseGet(() -> memberService.createNaverOauth(naverProfile, Provider.NAVER));

        // JWT 토큰 발급
        String jwtToken = jwtTokenProvider.createToken(originalMember.getId());

        return OAuth2LoginResponse.builder()
                .memberId(originalMember.getId())
                .jwt(jwtToken)
                .build();
    }

    /**
     * Postman 테스트용 메서드
     */
    public OAuth2LoginResponse testGetToken(AuthCodeToJwtRequest accessToken) {
        NaverProfile naverProfile = getNaverProfile(accessToken.getCode());

        Members originalMember = memberRepository.findBySnsId(naverProfile.getResponse().getId())
                .orElseGet(() -> memberService.createNaverOauth(naverProfile, Provider.NAVER));

        String jwtToken = jwtTokenProvider.createToken(originalMember.getId());

        return OAuth2LoginResponse.builder()
                .memberId(originalMember.getId())
                .jwt(jwtToken)
                .build();
    }

    // Access Token 획득 메서드
    // RestClient를 사용해서 네이버 서버와 통신
    private AccessToken getAccessToken(String code) {
        RestClient restClient = RestClient.create();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(CODE, code);
        params.add(CLIENT_ID, naverClientId);
        params.add(CLIENT_SECRET, naverClientSecret);
        params.add(REDIRECT_URI, naverRedirectUri);
        params.add(GRANT_TYPE, AUTHORIZATION_CODE);
        params.add(STATE, STATE_VALUE);

        ResponseEntity<AccessToken> response = restClient.post()
                .uri(ACCESS_TOKEN_NAVER_URI)
                .header(ACCESS_TOKEN_HEADER_NAME, ACCESS_TOKEN_HEADER_VALUE)
                .body(params)
                .retrieve()     // 응답 Body 값만 추출
                .toEntity(AccessToken.class);

        log.info("[NaverService.getAccessToken] : 응답 Access Token JSON = {}", response.getBody());
        return response.getBody();
    }

    // 사용자 정보 획득 메서드
    private NaverProfile getNaverProfile(String accessToken) {
        RestClient restClient = RestClient.create();

        ResponseEntity<NaverProfile> response = restClient.get()
                .uri(PROFILE_NAVER_URI)
                .header(PROFILE_HEADER_NAME, PROFILE_HEADER_VALUE + accessToken)
                .retrieve()
                .toEntity(NaverProfile.class);

        log.info("[NaverService.getNaverProfile] : 응답 profile JSON = {}", response.getBody());
        return response.getBody();
    }
}
