package com.trend_now.backend.post.application;

import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.domain.Scraps;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.repository.ScrapRepository;
import java.util.List;
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

    public List<PostsInfoDto> getScrappedPostsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Scraps> scraps = scrapRepository.findScrapsByMembers_Id(memberId, pageable);
        return scraps.stream()
            .map(scrap -> {
                Posts posts = scrap.getPosts();
                return PostsInfoDto.builder()
                    .title(posts.getTitle())
                    .writer(posts.getWriter())
                    .viewCount(posts.getViewCount())
                    .build();
            })
            .toList();
    }
}
