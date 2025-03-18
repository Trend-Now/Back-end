package com.trend_now.backend.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@DiscriminatorValue("guest_post")
public class GuestPost extends Post {

    @Column(nullable = false)
    private String guestName;

    @Column(nullable = false)
    private String guestPassword;
}
