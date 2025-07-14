package com.trend_now.backend.member.application;

import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.member.data.vo.*;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoService {

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    

    private static final String CODE = "code";
    private static final String CLIENT_ID = "client_id";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String GRANT_TYPE = "grant_type";
    private static final String AUTHORIZATION_CODE = "authorization_code";

    private static final String ACCESS_TOKEN_KAKAO_URI = "https://kauth.kakao.com/oauth/token";
    private static final String ACCESS_TOKEN_HEADER_NAME = "Content-Type";
    private static final String ACCESS_TOKEN_HEADER_VALUE = "application/x-www-form-urlencoded";

    private static final String PROFILE_KAKAO_URI = "https://kapi.kakao.com/v2/user/me";
    private static final String PROFILE_HEADER_NAME = "Authorization";
    private static final String PROFILE_HEADER_VALUE = "Bearer ";

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     *  getToken(AuthCodeToJwtRequest)
     *  - 인가 코드를 통해 JWT 토큰을 반환받는다.
     *  - 이 과정에는 인가 코드를 통해 Access Token을 발급 받고, Access Token을 통해 카카오로부터 사용자 정보를 받는다.
     *  - 사용자 정보를 본 서비스에서 검증하여 JWT 토큰을 발급해준다.
     */
    public OAuth2LoginResponse getToken(AuthCodeToJwtRequest authCodeToJwtRequest, HttpServletRequest request) {
        log.info("{}로 카카오 로그인 요청", request.getRequestURL());
        AccessToken accessToken = getAccessToken(authCodeToJwtRequest.getCode(), request);
        KakaoProfile kakaoProfile = getKakaoProfile(accessToken.getAccess_token());

        // socialId를 통해 회원 탐색(없으면 null 반환)
        // 최초 로그인 경우, 회원 정보 저장
        Members originalMember = memberRepository.findBySnsId(kakaoProfile.getId())
                .orElseGet(() -> memberService.createKakaoOauth(kakaoProfile, Provider.KAKAO));

        // JWT 토큰 발급
        String jwtToken = jwtTokenProvider.createToken(originalMember.getId());

        return OAuth2LoginResponse.builder()
                .memberId(originalMember.getId())
                .jwt(jwtToken)
                .build();
    }

    // Access Token 획득 메서드
    // RestClient를 사용해서 카카오 서버와 통신
    private AccessToken getAccessToken(String code, HttpServletRequest request) {
        RestClient restClient = RestClient.create();

        String redirectUri = ServletUriComponentsBuilder.fromRequest(request)
                .replacePath("/oauth/kakao/redirect")
                .build().toUriString();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(CODE, code);
        params.add(CLIENT_ID, kakaoClientId);
        params.add(REDIRECT_URI, redirectUri);
        params.add(GRANT_TYPE, AUTHORIZATION_CODE);

        ResponseEntity<AccessToken> response = restClient.post()
                .uri(ACCESS_TOKEN_KAKAO_URI)
                .header(ACCESS_TOKEN_HEADER_NAME, ACCESS_TOKEN_HEADER_VALUE)
                .body(params)
                .retrieve()     // 응답 Body 값만 추출
                .toEntity(AccessToken.class);

        log.info("[KakaoService.getAccessToken] : 응답 Access Token JSON = {}", response.getBody());
        return response.getBody();
    }

    // 사용자 정보 획득 메서드
    private KakaoProfile getKakaoProfile(String accessToken) {
        RestClient restClient = RestClient.create();

        ResponseEntity<KakaoProfile> response =  restClient.get()
                .uri(PROFILE_KAKAO_URI)
                .header(PROFILE_HEADER_NAME, PROFILE_HEADER_VALUE + accessToken)
                .retrieve()
                .toEntity(KakaoProfile.class);

        log.info("[KakaoService.getKakaoProfile] : 응답 profile JSON = {}", response.getBody());
        return response.getBody();
    }
}
