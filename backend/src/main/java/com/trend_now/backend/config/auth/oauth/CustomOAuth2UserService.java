package com.trend_now.backend.config.auth.oauth;

import com.trend_now.backend.config.auth.CustomUserDetails;
import com.trend_now.backend.member.application.MemberService;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 현재 로그인 시도 중인 OAuth 제공자를 가져온다 (예: google, kakao, naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // OAuth2User에서 사용자 식별 ID를 가져온다 (예: Google의 경우 'sub', Kakao의 경우 'id')
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        log.info("OAuth 로그인 종류: {}, 사용자 식별 ID: {}", registrationId, userNameAttributeName);

        // provider에 맞는 OAuthAttributes 객체 생성
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());
        // 회원가입이 되어 있지 않은 유저라면 새로 저장하고, 이미 존재하는 유저라면 정보를 업데이트 해서 저장한다.
        Members members = memberService.saveOrUpdate(attributes);

        return new CustomUserDetails(members, oAuth2User.getAttributes(), null, null, userNameAttributeName);
    }
}
