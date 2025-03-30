package com.trend_now.backend.member.application;

import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.member.data.vo.AccessToken;
import com.trend_now.backend.member.data.vo.GoogleLoginResponse;
import com.trend_now.backend.member.data.vo.GoogleProfile;
import com.trend_now.backend.member.data.vo.AuthCodeToJwtRequest;
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
public class GoogleService {

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    private static final String googleUri = "https://oauth2.googleapis.com/token";


    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     *  getToken(AuthCodeToJwtRequest)
     *  - 인가 코드를 통해 JWT 토큰을 반환받는다.
     *  - 이 과정에는 인가 코드를 통해 Access Token을 발급 받고, Access Token을 통해 구글로부터 사용자 정보를 받는다.
     *  - 사용자 정보를 본 서비스에서 검증하여 JWT 토큰을 발급해준다.
     */
    public GoogleLoginResponse getToken(AuthCodeToJwtRequest authCodeToJwtRequest) {
        AccessToken accessToken = getAccessToken(authCodeToJwtRequest.getCode());
        GoogleProfile googleProfile = getGoogleProfile(accessToken.getAccess_token());

        // socialId를 통해 회원 탐색(없으면 null 반환)
        Members originalMember = memberRepository.findBySnsId(googleProfile.getSub()).orElse(null);

        // 최초 로그인인 경우, 회원 정보 저장
        if(originalMember == null) {
            originalMember = memberService.createOauth(googleProfile, Provider.GOOGLE);
        }

        // JWT 토큰 발급
        String jwtToken = jwtTokenProvider.createToken(originalMember.getId());

        GoogleLoginResponse googleLoginResponse = GoogleLoginResponse.builder()
                .memberId(originalMember.getId())
                .jwt(jwtToken)
                .build();

        return googleLoginResponse;
    }

    // Access Token 획득 메서드
    // RestClient를 사용해서 구글 서버와 통신
    private AccessToken getAccessToken(String code) {
        RestClient restClient = RestClient.create();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<AccessToken> response = restClient.post()
                .uri(googleUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(params)
                .retrieve()     // 응답 Body 값만 추출
                .toEntity(AccessToken.class);

        log.info("[GoogleService.getAccessToken] : 응답 Access Token JSON = {}", response.getBody());
        return response.getBody();
    }

    // 사용자 정보 획득 메서드
    private GoogleProfile getGoogleProfile(String accessToken) {
        RestClient restClient = RestClient.create();

        ResponseEntity<GoogleProfile> response =  restClient.get()
                .uri("https://openidconnect.googleapis.com/v1/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toEntity(GoogleProfile.class);

        log.info("[GoogleService.getGoogleProfile] : 응답 profile JSON = {}", response.getBody());
        return response.getBody();
    }
}
