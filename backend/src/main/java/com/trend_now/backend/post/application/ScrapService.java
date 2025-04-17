package com.trend_now.backend.post.application;

import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostListDto;
import com.trend_now.backend.post.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final PostLikesService postLikesService;

    public Page<PostListDto> getScrappedPostsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Posts> scrapPosts = scrapRepository.findScrapPostsByMemberId(memberId, pageable);
        return scrapPosts.map(posts -> {
            int postLikesCount = postLikesService.getPostLikesCount(posts.getBoards().getId(),
                posts.getId());
            return PostListDto.of(posts, postLikesCount);
        });
    }
}
