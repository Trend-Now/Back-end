package com.trend_now.backend.comment.application;

import com.trend_now.backend.comment.data.dto.CommentInfoDto;
import com.trend_now.backend.comment.data.dto.DeleteCommentsDto;
import com.trend_now.backend.comment.data.dto.SaveCommentsDto;
import com.trend_now.backend.comment.data.dto.UpdateCommentsDto;
import com.trend_now.backend.comment.domain.BoardTtlStatus;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.exception.CustomException.BoardTtlException;
import com.trend_now.backend.exception.CustomException.InvalidRequestException;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentsService {

    private static final String NOT_EXIST_POSTS = "선택하신 게시글이 존재하지 않습니다.";
    private static final String NOT_EXIST_COMMENTS = "댓글이 없습니다.";
    private static final String BOARD_TTL_EXPIRATION = "게시판 활성 시간이 만료되었습니다.";
    private static final String BOARD_KEY_DELIMITER  = ":";
    private static final String NOT_COMMENT_WRITER = "댓글 작성자가 아닙니다.";
    private static final String NOT_EXIST_BOARD_TTL = "Redis에 BOARD_TTL 정보가 없습니다.";
    private static final String NOT_MODIFIABLE_POSTS = "게시글이 수정/삭제 불가능한 상태입니다.";

    private final CommentsRepository commentsRepository;
    private final PostsRepository postsRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 댓글 작성
     * - 회원만 가능, 컨트롤러에서 사용자 인증 객체 members를 받음
     * - BOARD_TTL 여부에 따라 BOARD_TTL_STATUS 컬럼 값이 결정
     */
    @Transactional
    public void saveComments(Members member, SaveCommentsDto saveCommentsDto) {
        Posts posts = postsRepository.findById(saveCommentsDto.getPostId())
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS)
                );

        BoardTtlStatus boardTtlStatus = checkBoardTtlStatus(saveCommentsDto.getBoardName(), saveCommentsDto.getBoardId());

        commentsRepository.save(Comments.builder()
                .content(saveCommentsDto.getContent())
                .members(member)
                .posts(posts)
                .boardTtlStatus(boardTtlStatus)
                .build());
    }

    /**
     * 댓글 삭제 조건
     * - 댓글 식별자에 맞는 댓글이 존재
     * - 본인 댓글만 삭제 가능
     * - 댓글은 BOARD_TTL 만료 시간 안에서만 삭제가 가능
     */
    @Transactional
    public void deleteCommentsByCommentId(Members member, DeleteCommentsDto deleteCommentsDto) {
        BoardTtlStatus boardTtlStatus = checkBoardTtlStatus(deleteCommentsDto.getBoardName(), deleteCommentsDto.getBoardId());

        // BOARD_TTL 만료 이전의 경우에만 삭제 가능
        if(boardTtlStatus.equals(BoardTtlStatus.BOARD_TTL_BEFORE)) {
            // 댓글 존재 확인
            Comments comments = commentsRepository.findById(deleteCommentsDto.getCommentId())
                    .orElseThrow(() -> new NotFoundException(NOT_EXIST_COMMENTS));
            if (!comments.isModifiable()) {
                throw new InvalidRequestException(NOT_MODIFIABLE_POSTS);
            }
            // 본인이 작성한 댓글만 삭제가 가능
            if(!comments.isCommentsWriter(comments, member)) {
                throw new InvalidRequestException(NOT_COMMENT_WRITER);
            }

            // 댓글 삭제 처리
            commentsRepository.deleteById(deleteCommentsDto.getCommentId());
        }
        // BOARD_TTL 만료 이후의 경우에는 댓글 삭제 불가능
        else if(boardTtlStatus.equals(BoardTtlStatus.BOARD_TTL_AFTER)) {
            throw new BoardTtlException(BOARD_TTL_EXPIRATION);
        }
        // BOARD_TTL 정보가 없는 경우에는 예외 처리
        // 절차 상, BOARD_TTL이 있어야 하나 없는 예외라 NotFoundException이 아닌 InvalidRequestException으로 처리
        else {
            throw new InvalidRequestException(NOT_EXIST_BOARD_TTL);
        }
    }

    /**
     * 댓글 수정 조건
     * - 댓글 식별자에 맞는 댓글이 존재
     * - 본인 댓글만 수정 가능
     * - 댓글은 BOART_TTL 만료 시간 안에서만 수정 가능
     */
    @Transactional
    public void updateCommentsByMembersAndCommentId(Members members, UpdateCommentsDto updateCommentsDto) {
        BoardTtlStatus boardTtlStatus = checkBoardTtlStatus(updateCommentsDto.getBoardName(), updateCommentsDto.getBoardId());

        // BOARD_TTL 만료 이전의 경우에만 수정 가능
        if(boardTtlStatus.equals(BoardTtlStatus.BOARD_TTL_BEFORE)) {
            // 댓글 존재 확인
            Comments comments = commentsRepository.findById(updateCommentsDto.getCommentId())
                    .orElseThrow(() -> new NotFoundException(NOT_EXIST_COMMENTS));

            if (!comments.isModifiable()) {
                throw new InvalidRequestException(NOT_MODIFIABLE_POSTS);
            }

            // 본인이 작성한 댓글만 수정이 가능
            if(!comments.isCommentsWriter(comments, members)) {
                throw new InvalidRequestException(NOT_COMMENT_WRITER);
            }

            // 댓글 수정 처리
            comments.update(updateCommentsDto);
        }
        // BOARD_TTL 만료 이후의 경우에는 댓글 수정 불가능
        else if(boardTtlStatus.equals(BoardTtlStatus.BOARD_TTL_AFTER)) {
            throw new BoardTtlException(BOARD_TTL_EXPIRATION);
        }
        // BOARD_TTL 정보가 없는 경우에는 예외 처리
        // 절차 상, BOARD_TTL이 있어야 하나 없는 예외라 NotFoundException이 아닌 InvalidRequestException으로 처리
        else {
            throw new InvalidRequestException(NOT_EXIST_BOARD_TTL);
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

    public Page<CommentInfoDto> getCommentsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentsRepository.findByMemberIdWithPost(memberId, pageable);
    }
}
