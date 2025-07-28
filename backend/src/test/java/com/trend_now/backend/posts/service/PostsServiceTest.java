package com.trend_now.backend.posts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trend_now.backend.board.application.BoardService;
import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.exception.CustomException.InvalidRequestException;
import com.trend_now.backend.exception.CustomException.NotFoundException;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.application.PostsService;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostSummaryDto;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.dto.PostsUpdateRequestDto;
import com.trend_now.backend.post.repository.PostsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
public class PostsServiceTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PostsService postsService;

    private Members members;
    private Boards boards;
    private Posts posts;
    private String key;

    @Autowired
    private PostsRepository postsRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private BoardService boardService;

    @BeforeEach
    public void before() throws InterruptedException {
        members = Members.builder()
            .name("testUser")
            .email("testEmail")
            .snsId("testSnsId")
            .provider(Provider.TEST)
            .build();
        memberRepository.save(members);

        boards = boardRepository.save(Boards.builder()
            .name("testBoard")
            .boardCategory(BoardCategory.REALTIME)
            .build());

        key = boards.getName() + ":" + boards.getId();
        redisTemplate.opsForValue().set(key, "0", 300L, TimeUnit.SECONDS);

        posts = Posts.builder()
            .title("testTitle")
            .writer(members.getName())
            .content("testContent")
            .boards(boards)
            .members(members)
            .build();
        Posts save = postsRepository.save(posts);
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("회원은 게시글을 작성할 수 있다.")
    public void 게시글_작성() throws Exception {
        //given
        PostsSaveDto postsSaveDto = PostsSaveDto.of("testTitle", "testContent", null);

        //when
        Long savePosts = postsService.savePosts(postsSaveDto, members, boards.getId());

        //then
        PostsInfoDto postsInfoDto = postsService.findPostsById(boards.getId(), savePosts, SecurityContextHolder.getContext().getAuthentication());
        assertThat(savePosts).isNotNull().isGreaterThan(0);
        assertThat(postsInfoDto.getTitle()).isEqualTo(postsSaveDto.getTitle());
        assertThat(postsInfoDto.getContent()).isEqualTo(postsSaveDto.getContent());
    }

    @ParameterizedTest
    @CsvSource({
        "1, 5",  // 첫 번째 페이지, 5개 게시글
        "2, 5",  // 두 번째 페이지, 5개 게시글
        "1, 10", // 첫 번째 페이지, 10개 게시글 (모두 가져오기)
    })
    @DisplayName("게시판별 게시글 페이징 조회")
    public void 게시글_페이징_조회(int page, int size) {
        //given
        for (int i = 1; i <= 10; i++) {
            PostsSaveDto postsSaveDto = PostsSaveDto.of("title" + i, "content" + i, null);
            postsService.savePosts(postsSaveDto, members, boards.getId());
        }
        PostsPagingRequestDto requestDto = PostsPagingRequestDto.of(boards.getId(), page - 1, size);

        //when
        Page<PostSummaryDto> result = postsService.findAllPostsPagingByBoardId(requestDto);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getContent().size()).isEqualTo(size);

        int allPosts = postsRepository.findAll().size();

        if (page == allPosts) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).isNotEmpty();
        }
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("작성자가 직접 작성한 게시글을 수정할 수 있다")
    public void 게시글_수정() throws Exception {
        //given
        PostsUpdateRequestDto postsUpdateRequestDto = PostsUpdateRequestDto.of("updateTitle",
            "updateContent", null, null);

        //when
        postsService.updatePostsById(postsUpdateRequestDto, boards.getId(), posts.getId(),
            members.getId());
        em.flush();
        em.clear();

        //then
        PostsInfoDto postsInfoDto = postsService.findPostsById(boards.getId(), posts.getId(), SecurityContextHolder.getContext().getAuthentication());
        assertThat(postsInfoDto.getTitle()).isEqualTo(postsUpdateRequestDto.getTitle());
        assertThat(postsInfoDto.getContent()).isEqualTo(postsUpdateRequestDto.getContent());
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("작성자가 직접 작성한 게시글을 삭제할 수 있다")
    public void 게시글_삭제() throws Exception {
        //given
        Long postId = posts.getId();

        //when
        postsService.deletePostsById(boards.getId(), postId, members.getId());
        em.flush();
        em.clear();

        //then
        assertThatThrownBy(() -> postsService.findPostsById(boards.getId(), posts.getId(), SecurityContextHolder.getContext().getAuthentication()))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("가변 타이머가 끝나면 게시글은 수정, 삭제가 불가능하다")
    public void 게시판의_타이머가_끝나면_게시글은_수정_삭제가_불가능하다() throws Exception {
        //given
        PostsSaveDto postsSaveDto = PostsSaveDto.of("testTitle", "testContent", null);
        postsService.savePosts(postsSaveDto, members, boards.getId());

        //when
        redisTemplate.delete(key);
        PostsUpdateRequestDto postsUpdateRequestDto = PostsUpdateRequestDto.of("updateTitle",
            "updateContent", null, null);

        //then
        assertThatThrownBy(
            () -> postsService.deletePostsById(boards.getId(), posts.getId(), members.getId()))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("타이머가 종료된 게시판입니다. 타이머가 남아있는 게시판에서만 요청할 수 있습니다.");
        assertThatThrownBy(() -> postsService.updatePostsById(postsUpdateRequestDto,
            boards.getId(), posts.getId(), members.getId()))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("타이머가 종료된 게시판입니다. 타이머가 남아있는 게시판에서만 요청할 수 있습니다.");
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("게시판이 재등록 시 기존에 작성된 게시글은 수정, 삭제가 불가능하다")
    public void 게시판_재등록_시_기존에_작성된_게시글은_수정_삭제가_불가능하다() throws Exception {
        //given
        PostsSaveDto postsSaveDto = PostsSaveDto.of("testTitle", "testContent", null);
        Long postId = postsService.savePosts(postsSaveDto, members, boards.getId());

        //when
        // 게시판 삭제
        redisTemplate.delete(key);
        BoardSaveDto boardSaveDto = new BoardSaveDto(boards.getId(), boards.getName(), boards.getBoardCategory());
        boardService.updateBoardIsDeleted(boardSaveDto, false);
        postsService.updateModifiable(boards.getId()); // SignalKeywordJobListener에서 하는 일 직접 실행
        em.flush();
        em.clear();
        // 게시판 재등록
        boardService.updateBoardIsDeleted(boardSaveDto, true);
        redisTemplate.opsForValue().set(key, "testBoard", 300L);

        //then
        boolean modifiable = postsService.findPostsById(boards.getId(), postId, SecurityContextHolder.getContext().getAuthentication()).isModifiable();
        assertThat(modifiable).isFalse();

        //then
        // 게시글 수정
        PostsUpdateRequestDto postsUpdateRequestDto = PostsUpdateRequestDto.of("updateTitle",
            "updateContent", null, null);
        assertThatThrownBy(() -> postsService.updatePostsById(postsUpdateRequestDto,
            boards.getId(), posts.getId(), members.getId()))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("게시글이 수정/삭제 불가능한 상태입니다.");
        // 게시글 삭제
        assertThatThrownBy(
            () -> postsService.deletePostsById(boards.getId(), posts.getId(), members.getId()))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("게시글이 수정/삭제 불가능한 상태입니다.");
    }


}
