package com.trend_now.backend.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.data.dto.DeleteCommentsDto;
import com.trend_now.backend.comment.data.dto.SaveCommentsDto;
import com.trend_now.backend.comment.data.dto.UpdateCommentsDto;
import com.trend_now.backend.comment.domain.BoardTtlStatus;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
import com.trend_now.backend.exception.CustomException.BoardTtlException;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.repository.PostsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.yml")
class CommentsServiceTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CommentsService commentsService;

    @Autowired
    private CommentsRepository commentsRepository;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BoardRepository boaardRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Posts testPost;
    private Members testMembers;
    private Boards testBoards;

    private static final String BOARD_KEY_DELIMITER = ":";

    @BeforeEach
    public void setUp() {
        testMembers = memberRepository.save(Members.builder()
                .name("testUser")
                .email("testEmail")
                .snsId("testSnsId")
                .provider(Provider.TEST)
                .build());

        testBoards = boaardRepository.save(Boards.builder()
                .name("testBoards")
                .boardCategory(BoardCategory.REALTIME)
                .build());

        testPost = postsRepository.save(Posts.builder()
                .title("testTitle")
                .writer("testWriter")
                .content("testContent")
                .boards(testBoards)
                .members(testMembers)
                .build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("BOARD_TTL 존재 여부에 따라 boardTtlStatus가 다르게 저장된다")
    void BOARD_TTL_여부에_따라_boardTtlStatus가_결정() {
        // given
        // redis에 게시판 저장
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testPost.getId();
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        SaveCommentsDto testSaveCommentsDto =
                SaveCommentsDto.of(testBoards.getId(), testPost.getId(), testBoards.getName(), "testContent");

        // when
        // BOARD_TTL 존재 했을 때, 댓글을 작성
        commentsService.saveComments(testMembers, testSaveCommentsDto);

        // Redis에서 키 삭제하여 TTL 없는 상태로 만듦
        redisTemplate.delete(key);

        // BOARD_TTL 존재하지 않았을 때, 댓글을 작성
        commentsService.saveComments(testMembers, testSaveCommentsDto);

        // then
        List<Comments> commentsList = commentsRepository.findAll();

        assertThat(commentsList).hasSize(2);

        Comments beforeTtlComments = commentsList.get(0);
        Comments afterTtlComments = commentsList.get(1);

        System.out.println("beforeTtlComments : " + beforeTtlComments.toString());
        System.out.println("afterTtlComments : " + afterTtlComments.toString());

        // beforeTtlComment의 BoardTtlStatus는 "BOARD_TTL_BEFORE" 값을 가짐
        assertThat(beforeTtlComments.getBoardTtlStatus()).isEqualTo(BoardTtlStatus.BOARD_TTL_BEFORE);

        // afterTtlComment의 BoardTtlStatus는 "BOARD_TTL_AFTER" 값을 가짐
        assertThat(afterTtlComments.getBoardTtlStatus()).isEqualTo(BoardTtlStatus.BOARD_TTL_AFTER);

    }

    @Test
    @DisplayName("BOARD_TTL 존재 여부에 따라 댓글 삭제가 결정된다")
    void BOARD_TTL_여부에_따라_댓글_삭제_결정() {
        // given
        // redis에 게시판 저장 및 댓글 2개 저장
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testPost.getId();
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        SaveCommentsDto testSaveCommentsDto =
                SaveCommentsDto.of(testBoards.getId(), testPost.getId(), testBoards.getName(), "testContent");

        commentsService.saveComments(testMembers, testSaveCommentsDto);
        commentsService.saveComments(testMembers, testSaveCommentsDto);

        List<Comments> commentsList = commentsRepository.findAll();
        Comments comments1 = commentsList.get(0);
        Comments comments2 = commentsList.get(1);

        DeleteCommentsDto deleteCommentsDto1 = DeleteCommentsDto.of(
                testBoards.getId(), testPost.getId(), testBoards.getName(), comments1.getId());

        DeleteCommentsDto deleteCommentsDto2 = DeleteCommentsDto.of(
                testBoards.getId(), testPost.getId(), testBoards.getName(), comments2.getId());


        // when & then
        // 하나는 BOARD_TTL 전에 삭제하여 삭제됨을 확인 & 나머지 하나는 BOARD_TTL 후에 삭제하지만 삭제안됨을 확인
        commentsService.deleteCommentsByCommentId(testMembers, deleteCommentsDto1);

        // Redis에서 키 삭제하여 TTL 없는 상태로 만듦
        redisTemplate.delete(key);

        assertThatThrownBy(() -> commentsService.deleteCommentsByCommentId(testMembers, deleteCommentsDto2))
                .isInstanceOf(BoardTtlException.class)
                .hasMessage("게시판 활성 시간이 만료되었습니다.");

        commentsList = commentsRepository.findAll();
        System.out.println("commentsList >>> " + commentsList.toString());
        assertThat(commentsList.size()).isEqualTo(1);
        assertThat(commentsList.get(0)).isEqualTo(comments2);
    }

    @Test
    @DisplayName("BOARD_TTL 존재 여부에 따라 댓글 수정이 결정된다")
    void BOARD_TTL_여부에_따라_댓글_수정_결정() {
        // given
        // redis에 게시판 저장 및 댓글 2개 저장
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testPost.getId();
        redisTemplate.opsForValue().set(key, "실시간 게시판");

        SaveCommentsDto testSaveCommentsDto =
                SaveCommentsDto.of(testBoards.getId(), testPost.getId(), testBoards.getName(), "testContent");

        commentsService.saveComments(testMembers, testSaveCommentsDto);
        commentsService.saveComments(testMembers, testSaveCommentsDto);

        List<Comments> commentsList = commentsRepository.findAll();
        Comments comments1 = commentsList.get(0);
        Comments comments2 = commentsList.get(1);

        UpdateCommentsDto updateCommentsDto1 = UpdateCommentsDto.of(
                testBoards.getId(), testPost.getId(), testBoards.getName(), comments1.getId(), "test updated comments1");

        UpdateCommentsDto updateCommentsDto2 = UpdateCommentsDto.of(
                testBoards.getId(), testPost.getId(), testBoards.getName(), comments2.getId(), "test updated comments1");


        // when & then
        // 하나는 BOARD_TTL 전에 수정하여 수정됨을 확인 & 나머지 하나는 BOARD_TTL 후에 수정하지만 수정안됨을 확인
        commentsService.updateCommentsByMembersAndCommentId(testMembers, updateCommentsDto1);

        // Redis에서 키 삭제하여 TTL 없는 상태로 만듦
        redisTemplate.delete(key);

        assertThatThrownBy(() -> commentsService.updateCommentsByMembersAndCommentId(testMembers, updateCommentsDto2))
                .isInstanceOf(BoardTtlException.class)
                .hasMessage("게시판 활성 시간이 만료되었습니다.");

        commentsList = commentsRepository.findAll();
        System.out.println("commentsList >>> " + commentsList.toString());
        assertThat(commentsList.size()).isEqualTo(2);

        // 댓글1은 수정이 되고, 댓글2는 수정이 안됨을 확인
        assertThat(commentsList.get(0).getContent()).isEqualTo("test updated comments1");
        assertThat(commentsList.get(1).getContent()).isEqualTo("testContent");

    }
}