package com.trend_now.backend.post.application;

import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.repository.ScrapRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    public Page<PostSummaryDto> getScrappedPostsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Posts> scrapPosts = scrapRepository.findScrapPostsByMemberId(memberId, pageable);

        List<PostSummaryDto> postSummaryDtoList = scrapPosts.getContent().stream().map(post -> {
            // 게시글 좋아요 개수 조회
            int postLikesCount = postLikesService.getPostLikesCount(post.getBoards().getId(),
                post.getId());
            return PostSummaryDto.of(post, postLikesCount);
        }).toList();

        return new PageImpl<>(postSummaryDtoList, pageable, scrapPosts.getTotalElements());
    }
}
