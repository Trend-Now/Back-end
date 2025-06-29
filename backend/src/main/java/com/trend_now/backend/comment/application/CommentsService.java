package com.trend_now.backend.comment.application;

import com.trend_now.backend.board.application.BoardRedisService;
import com.trend_now.backend.comment.data.dto.CommentInfoDto;
import com.trend_now.backend.comment.data.dto.DeleteCommentsDto;
import com.trend_now.backend.comment.data.dto.SaveCommentsDto;
import com.trend_now.backend.comment.data.dto.UpdateCommentsDto;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.exception.CustomException.BoardExpiredException;
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
    private static final String BOARD_EXPIRATION = "게시판 활성 시간이 만료되었습니다.";
    private static final String BOARD_KEY_DELIMITER  = ":";
    private static final String NOT_COMMENT_WRITER = "댓글 작성자가 아닙니다.";
    private static final String NOT_EXIST_BOARD_TTL = "Redis에 BOARD_TTL 정보가 없습니다.";
    private static final String NOT_MODIFIABLE_COMMENTS = "댓글이 수정/삭제 불가능한 상태입니다.";

    private final CommentsRepository commentsRepository;
    private final PostsRepository postsRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final BoardRedisService boardRedisService;

    /**
     * 댓글 작성
     * - 회원만 가능, 컨트롤러에서 사용자 인증 객체 members를 받음
     * - 게시판 활성화인 경우에만 댓글 작성 가능
     */
    @Transactional
    public Comments saveComments(Members member, SaveCommentsDto saveCommentsDto) {
        // 게시판 비활성화인 경우, 댓글 작성 불가능 예외
        if(!boardRedisService.isRealTimeBoard(saveCommentsDto)) {
            throw new BoardExpiredException(BOARD_EXPIRATION);
        }

        Posts posts = postsRepository.findById(saveCommentsDto.getPostId())
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_POSTS)
                );

        return commentsRepository.save(Comments.builder()
                .content(saveCommentsDto.getContent())
                .members(member)
                .posts(posts)
                .build());
    }

    /**
     * 댓글 삭제 조건
     * - 댓글 식별자에 맞는 댓글이 존재
     * - 본인 댓글만 삭제 가능
     */
    @Transactional
    public void deleteCommentsByCommentId(Members member, DeleteCommentsDto deleteCommentsDto) {
        // 댓글 존재 확인
        Comments comments = commentsRepository.findById(deleteCommentsDto.getCommentId())
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_COMMENTS));

        // 본인이 작성한 댓글만 삭제가 가능
        if(!comments.isCommentsWriter(comments, member)) {
            throw new InvalidRequestException(NOT_COMMENT_WRITER);
        }

        // modifiable이 true인 경우에만 삭제 가능
        if (!comments.isModifiable()) {
            throw new InvalidRequestException(NOT_MODIFIABLE_COMMENTS);
        }

        // 댓글 삭제 처리
        commentsRepository.deleteById(deleteCommentsDto.getCommentId());
    }

    /**
     * 댓글 수정 조건
     * - 댓글 식별자에 맞는 댓글이 존재
     * - 본인 댓글만 수정 가능
     */
    @Transactional
    public void updateCommentsByMembersAndCommentId(Members members, UpdateCommentsDto updateCommentsDto) {
        // 댓글 존재 확인
        Comments comments = commentsRepository.findById(updateCommentsDto.getCommentId())
                .orElseThrow(() -> new NotFoundException(NOT_EXIST_COMMENTS));

        // 본인이 작성한 댓글만 수정이 가능
        if(!comments.isCommentsWriter(comments, members)) {
            throw new InvalidRequestException(NOT_COMMENT_WRITER);
        }

        // modifiable이 true인 경우에만 삭제 가능
        if (!comments.isModifiable()) {
            throw new InvalidRequestException(NOT_MODIFIABLE_COMMENTS);
        }

        // 댓글 수정 처리
        comments.update(updateCommentsDto);
    }

    public Page<CommentInfoDto> getCommentsByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentsRepository.findByMemberIdWithPost(memberId, pageable);
    }

    /**
     * 게시글이 속한 게시판의 타이머가 만료됐을 경우 modifiable 필드의 값을 false로 변경합니다.
     * modifiable = false가 수정/삭제 불가능
     */
    @Transactional
    public void updateModifiable(Long boardId) {
        commentsRepository.updateFlagByBoardId(boardId);
    }
}
