package com.trend_now.backend.comment.application;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.comment.data.vo.SaveComments;
import com.trend_now.backend.comment.domain.BoardTtlStatus;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentsService {

    private static final String NOT_EXIST_POSTS = "선택하신 게시글이 존재하지 않습니다.";
    private static final String NOT_EXIST_Members = "회원이 아닙니다.";
    private static final String BOARD_KEY_DELIMITER  = ":";

    private final CommentsRepository commentsRepository;
    private final PostsRepository postsRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 댓글 작성
     * - 회원만 가능, 컨트롤러에서 사용자 인증 객체 members를 받음
     * - BOARD_TTL 여부에 따라 BOARD_TTL_STATUS 컬럼 값이 결정
     */
    @Transactional
    public void saveComments(Members member, SaveComments saveComments) {
        // 회원 확인
        if(member == null) {
            throw new NotFoundException(NOT_EXIST_Members);
        }

        Posts posts = postsRepository.findById(saveComments.getPostId())
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS)
                );

        // RedisTemplate를 통해 BOARD_TTL 여부 확인
        // key는 게시글의 이름과 게시글 식별자로 이루어진다.
        String key = saveComments.getBoardName() + BOARD_KEY_DELIMITER + saveComments.getBoardId();
        BoardTtlStatus boardTtlStatus = redisTemplate.hasKey(key)
                ? BoardTtlStatus.BOARD_TTL_BEFORE : BoardTtlStatus.BOARD_TTL_AFTER;

        commentsRepository.save(Comments.builder()
                .content(saveComments.getContent())
                .members(member)
                .posts(posts)
                .boardTtlStatus(boardTtlStatus)
                .build());
    }
}
