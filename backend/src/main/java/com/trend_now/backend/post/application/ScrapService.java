package com.trend_now.backend.post.application;

import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.application.MemberService;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.domain.Scraps;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.repository.PostsRepository;
import com.trend_now.backend.post.repository.ScrapRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final PostLikesService postLikesService;
    private final MemberRepository memberRepository;
    private final PostsRepository postsRepository;

    private static final String NOT_EXIST_MEMBER = "해당 회원이 존재하지 않습니다.";
    private static final String NOT_EXIST_POST = "해당 게시글이 존재하지 않습니다.";

    @Transactional
    public void scrapPost(Long memberId, Long postId) {
        Members members = memberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_MEMBER));
        Posts posts = postsRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException(NOT_EXIST_POST));

        Optional<Scraps> optionalScraps = scrapRepository.findByMembersAndPosts(members, posts);
        if(optionalScraps.isPresent()) {
            // 이미 스크랩한 게시글인 경우
            scrapRepository.delete(optionalScraps.get());
            log.info("{}번 회원이 {}번 게시글을 스크랩 취소했습니다.", memberId, postId);
        } else {
            // 스크랩하지 않은 게시글인 경우
            Scraps scrap = Scraps.builder()
                .members(members)
                .posts(posts)
                .build();

            scrapRepository.save(scrap);
            log.info("{}번 게시글을(를) {}번 회원이 스크랩했습니다.", postId, memberId);
        }

    }

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
