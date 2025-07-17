package com.trend_now.backend.config.auth.oauth;

import com.trend_now.backend.member.application.MemberService;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String email;
    private String snsId;
    private Provider provider;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String email, String snsId, Provider provider) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.email = email;
        this.snsId = snsId;
        this.provider = provider;
    }

    public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> ofGoogle(attributes);
            case "naver" -> ofNaver(attributes);
            case "kakao" -> ofKakao(attributes);
            default ->
                throw new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: " + registrationId);
        };
    }

    private static OAuthAttributes ofGoogle(Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .email((String) attributes.get("email"))
                .snsId((String) attributes.get("sub"))
                .provider(Provider.GOOGLE)
                .attributes(attributes)
                .build();
    }

    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuthAttributes.builder()
                .email((String) response.get("email"))
                .snsId((String) response.get("id"))
                .provider(Provider.NAVER)
                .attributes(response)
                .build();
    }

    private static OAuthAttributes ofKakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        return OAuthAttributes.builder()
                .email((String) kakaoAccount.get("email"))
                .snsId(String.valueOf(attributes.get("id")))
                .provider(Provider.KAKAO)
                .attributes(attributes)
                .build();
    }

    public Members toEntity() {
        return Members.builder()
                .name(MemberService.createNickname())
                .email(email)
                .snsId(snsId)
                .provider(provider)
                .role(Role.USER)
                .build();
    }
}
