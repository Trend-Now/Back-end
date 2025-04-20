package com.trend_now.backend.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.data.vo.SaveComments;
import com.trend_now.backend.comment.domain.BoardTtlStatus;
import com.trend_now.backend.comment.domain.Comments;
import com.trend_now.backend.comment.repository.CommentsRepository;
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

import static org.junit.jupiter.api.Assertions.*;

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

    private static final String BOARD_KEY_DELIMITER  = ":";

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
    @DisplayName("회원만 댓글 작성이 가능하다")
    void 비회원_댓글_작성_불가능() {
        // given
        // 로그인 안한 상태로 댓글 작성을 요청
        SaveComments saveComments = SaveComments.builder()
                .postId(testPost.getId())
                .boardId(testPost.getId())
                .boardName(testBoards.getName())
                .content("testContent")
                .build();

        // when & then
        // 유저 인증 객체 members에 대한 정보가 null 이므로 댓글 작성이 불가능해야 한다.
        assertThatThrownBy(() -> commentsService.saveComments(null, saveComments))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("회원이 아닙니다.");
    }

    @Test
    @DisplayName("BOARD_TTL 존재 여부에 따라 boardTtlStatus가 다르게 저장된다")
    void checkBoardTtlStatusChange() {
        // given
        // redis에 게시판 저장
        String key = testBoards.getName() + BOARD_KEY_DELIMITER + testPost.getId();
        redisTemplate.opsForValue().set(key, "1");

        SaveComments beforeSaveComments = SaveComments.builder()
                .postId(testPost.getId())
                .boardId(testPost.getId())
                .boardName(testBoards.getName())
                .content("BOARD_TTL_BEFORE testContent")
                .build();

        SaveComments afterSaveComments = SaveComments.builder()
                .postId(testPost.getId())
                .boardId(testPost.getId())
                .boardName(testBoards.getName())
                .content("BOARD_TTL_AFTER testContent")
                .build();

        // when
        // BOARD_TTL 존재 했을 때, 댓글을 작성
        commentsService.saveComments(testMembers, beforeSaveComments);

        // Redis에서 키 삭제하여 TTL 없는 상태로 만듦
        redisTemplate.delete(key);

        // BOARD_TTL 존재하지 않았을 때, 댓글을 작성
        commentsService.saveComments(testMembers, afterSaveComments);

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
}