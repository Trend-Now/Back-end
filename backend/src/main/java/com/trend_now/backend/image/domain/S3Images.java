package com.trend_now.backend.image.domain;

import com.trend_now.backend.aws.s3.domain.S3File;
import com.trend_now.backend.post.domain.Posts;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class S3Images extends S3File {

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Posts posts;

    public S3Images(String s3key, String imageUrl, Posts posts) {
        super(s3key);
        this.imageUrl = imageUrl;
        this.posts = posts;
    }
}
