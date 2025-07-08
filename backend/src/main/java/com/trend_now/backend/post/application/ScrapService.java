package com.trend_now.backend.post.application;

import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.domain.ScrapAction;
import com.trend_now.backend.post.domain.Scraps;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
import com.trend_now.backend.post.repository.ScrapRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final MemberRepository memberRepository;
    private final PostsRepository postsRepository;
    private final PostViewService postViewService;
    private final PostLikesService postLikesService;

    private static final String NOT_EXIST_MEMBER = "해당 회원이 존재하지 않습니다.";
    private static final String NOT_EXIST_POST = "해당 게시글이 존재하지 않습니다.";

    @Transactional
    public ScrapAction scrapPost(Long memberId, Long postId) {
        Members requestMember = memberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_MEMBER));
        Posts posts = postsRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_POST));

        Optional<Scraps> optionalScraps = scrapRepository.findByMembersAndPosts(requestMember, posts);
        if(optionalScraps.isPresent()) {
            // 이미 스크랩한 게시글인 경우
            scrapRepository.delete(optionalScraps.get());
            log.info("{}번 회원이 {}번 게시글을 스크랩 취소했습니다.", memberId, postId);
            return ScrapAction.UNSCRAPPED;
        } else {
            // 스크랩하지 않은 게시글인 경우
            Scraps scrap = Scraps.builder()
                .members(requestMember)
                .posts(posts)
                .build();

            scrapRepository.save(scrap);
            log.info("{}번 게시글을(를) {}번 회원이 스크랩했습니다.", postId, memberId);
            return ScrapAction.SCRAPPED;
        }

    }

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

    public boolean isScrapedPost(Long id, Long postId) {
        return scrapRepository.existsScrapsByPosts_IdAndMembers_Id(postId, id);
    }
}
