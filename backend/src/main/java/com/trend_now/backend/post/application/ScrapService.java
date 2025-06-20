package com.trend_now.backend.post.application;

import com.trend_now.backend.post.dto.PostWithBoardSummaryDto;
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

    public Page<PostWithBoardSummaryDto> getScrappedPostsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return scrapRepository.findScrapPostsByMemberId(memberId, pageable);
    }
}
