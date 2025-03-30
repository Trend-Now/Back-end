package com.trend_now.backend.member.data.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *  Kakao 사용자 정보 형식
 *  {
 *   "id": 123,
 *   "connected_at": "2025-03-30T09:41:47Z",
 *   "properties": {
 *     "nickname": "박찬웅",
 *     "profile_image": "a.jpg",
 *     "thumbnail_image": "a.jpg"
 *   },
 *   "kakao_account": {
 *     "profile_nickname_needs_agreement": false,
 *     "profile_image_needs_agreement": false,
 *     "profile": {
 *       "nickname": "박찬웅",
 *       "thumbnail_image_url": "a.jpg",
 *       "profile_image_url": "a.jpg",
 *       "is_default_image": false,
 *       "is_default_nickname": false
 *     },
 *     "has_email": true,
 *     "email_needs_agreement": false,
 *     "is_email_valid": true,
 *     "is_email_verified": true,
 *     "email": "email@nate.com"
 *   }
 * }
 *
 * todo. 필요한 사용자 정보는 추가 가능
 */
@AllArgsConstructor
@Data
// JsonIgnoreProperties 어노테이션을 사용하면 응답 받는 JSON 프로퍼티 중 여기에 정의되지 않은 것들에 의한 에러를 막아준다.
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoProfile {

    private final String id;       // id은 social_id를 의미
    private final KakaoAccount kakao_account;

    @AllArgsConstructor
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {
        private final String email;
        private final Profile profile;
    }

    @AllArgsConstructor
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile{
        private String nickname;
        private String profile_image_url;
    }
}
