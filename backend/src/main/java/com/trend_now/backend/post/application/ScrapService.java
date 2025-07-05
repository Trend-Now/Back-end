package com.trend_now.backend.post.application;

import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import com.trend_now.backend.post.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final PostViewService postViewService;
    private final PostLikesService postLikesService;

    public Page<PostWithBoardSummaryDto> getScrappedPostsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Direction.DESC, "createdAt"));
        Page<PostWithBoardSummaryDto> scrapPostsByMemberId = scrapRepository.findScrapPostsByMemberId(
            memberId, pageable);

        // 만약 redis에 저장된 게시글 조회수와 게시글 좋아요 수가 있다면, 해당 조회수를 PostSummaryDto에 설정 (Look Aside)
        scrapPostsByMemberId.forEach(scrapPost -> {
            int postViewCount = postViewService.getPostViewCount(scrapPost.getPostId());
            scrapPost.setViewCount(postViewCount);
            int postLikesCount = postLikesService.getPostLikesCount(scrapPost.getBoardId(),
                scrapPost.getPostId());
            scrapPost.setLikeCount(postLikesCount);
        });

        return scrapPostsByMemberId;
    }
}
