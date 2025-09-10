package com.trend_now.backend.post.dto;

import com.trend_now.backend.image.dto.ImageInfoDto;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PostInfoResponseDto {

    private final PostsInfoDto postInfoDto;
    private final List<ImageInfoDto> imageInfos;

    public static PostInfoResponseDto of(PostsInfoDto postInfoDto, List<ImageInfoDto> imageInfos) {
        return new PostInfoResponseDto(postInfoDto, imageInfos);
    }
}
