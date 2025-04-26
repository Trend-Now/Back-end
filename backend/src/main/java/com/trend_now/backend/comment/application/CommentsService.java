package com.trend_now.backend.comment.application;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.comment.data.vo.DeleteComments;
import com.trend_now.backend.comment.data.vo.SaveComments;
import com.trend_now.backend.comment.data.vo.UpdateComments;
import com.trend_now.backend.comment.domain.BoardTtlStatus;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.exception.CustomException.BoardTtlException;
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
    private static final String NOT_EXIST_MEMBERS = "회원이 아닙니다.";
    private static final String NOT_EXIST_COMMENTS = "댓글이 없습니다.";
    private static final String BOARD_TTL_EXPIRATION = "게시판 활성 시간이 만료되었습니다.";
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
        checkMemberExist(member);

        Posts posts = postsRepository.findById(saveComments.getPostId())
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS)
                );

        BoardTtlStatus boardTtlStatus = checkBoardTtlStatus(saveComments.getBoardName(), saveComments.getBoardId());

        commentsRepository.save(Comments.builder()
                .content(saveComments.getContent())
                .members(member)
                .posts(posts)
                .boardTtlStatus(boardTtlStatus)
                .build());
    }

    /**
     * 댓글 삭제 조건
     * - 본인 댓글만 삭제 가능
     * - 댓글은 BOARD_TTL 만료 시간 안에서만 삭제가 가능
     */
    @Transactional
    public void deleteCommentsByCommentId(Members member, DeleteComments deleteComments) {
        checkMemberExist(member);
        BoardTtlStatus boardTtlStatus = checkBoardTtlStatus(deleteComments.getBoardName(), deleteComments.getBoardId());

        // BOARD_TTL 만료 이전의 경우에만 삭제 가능
        if(boardTtlStatus.equals(BoardTtlStatus.BOARD_TTL_BEFORE)) {
            commentsRepository.deleteByIdAndMembers(deleteComments.getCommentId(), member);
        } else {
            throw new BoardTtlException(BOARD_TTL_EXPIRATION);
        }
    }

    /**
     * 댓글 수정 조건
     * - 본인 댓글만 수정 가능
     * - 댓글은 BOART_TTL 만료 시간 안에서만 수정 가능
     */
    @Transactional
    public void updateCommentsByMembersAndCommentId(Members members, UpdateComments updateComments) {
        checkMemberExist(members);
        BoardTtlStatus boardTtlStatus = checkBoardTtlStatus(updateComments.getBoardName(), updateComments.getBoardId());

        // BOARD_TTL 만료 이전의 경우에만 수정 가능
        if(boardTtlStatus.equals(BoardTtlStatus.BOARD_TTL_BEFORE)) {
            Comments comments = commentsRepository.findByIdAndMembers(updateComments.getCommentId(), members)
                    .orElseThrow(() -> new NotFoundException(NOT_EXIST_COMMENTS));

            // 댓글 수정
            comments.update(updateComments);
            commentsRepository.save(comments);
        } else {
            throw new BoardTtlException(BOARD_TTL_EXPIRATION);
        }
    }

    /**
     * 사용자 인증 객체 Members 가 존재하는 지 확인하는 메서드
     */
    private void checkMemberExist(Members member) {
        // 회원 확인
        if(member == null) {
            throw new NotFoundException(NOT_EXIST_MEMBERS);
        }
    }

    /**
     * BOARD_TTL_STATUS 확인 메서드
     */
    private BoardTtlStatus checkBoardTtlStatus(String boardName, Long boardId) {
        String key = boardName + BOARD_KEY_DELIMITER + boardId;
        return redisTemplate.hasKey(key)
                ? BoardTtlStatus.BOARD_TTL_BEFORE : BoardTtlStatus.BOARD_TTL_AFTER;
    }
}
