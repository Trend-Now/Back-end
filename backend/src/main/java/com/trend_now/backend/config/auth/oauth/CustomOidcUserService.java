package com.trend_now.backend.config.auth.oauth;

import com.trend_now.backend.config.auth.CustomUserDetails;
import com.trend_now.backend.member.application.MemberService;
import com.trend_now.backend.member.domain.Members;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Google은 OIDC 인증 프로토콜을 사용하기 때문에 OidcUserService를 따로 구현하여 사용자 정보를 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final MemberService memberService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
            .getUserInfoEndpoint().getUserNameAttributeName();

        log.info("OIDC 로그인 종류: {}, 사용자 식별 ID: {}", registrationId, userNameAttributeName);

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, oidcUser.getAttributes());
        Members members = memberService.saveOrUpdate(attributes);

        return new CustomUserDetails(
            members,
            oidcUser.getAttributes(),
            oidcUser.getIdToken(),
            oidcUser.getUserInfo(),
            userNameAttributeName
        );
    }
}
