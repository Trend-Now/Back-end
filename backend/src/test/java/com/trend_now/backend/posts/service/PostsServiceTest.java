package com.trend_now.backend.posts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.member.repository.MemberRepository;
import com.trend_now.backend.post.application.PostsService;
import com.trend_now.backend.post.domain.Post;
import com.trend_now.backend.post.domain.Posts;
import com.trend_now.backend.post.dto.PostsDeleteDto;
import com.trend_now.backend.post.dto.PostsInfoDto;
import com.trend_now.backend.post.dto.PostsPagingRequestDto;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.dto.PostsUpdateDto;
import com.trend_now.backend.post.repository.PostsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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

    @Autowired
    private PostsRepository postsRepository;

    @BeforeEach
    public void before() {
        members = Members.builder()
                .name("testUser")
                .email("testEmail")
                .snsId("testSnsId")
                .provider(Provider.TEST)
                .build();
        memberRepository.save(members);

        boards = Boards.builder()
                .name("testBoard")
                .boardCategory(BoardCategory.REALTIME)
                .build();
        boardRepository.save(boards);

        for (int i = 1; i <= 10; i++) {
            PostsSaveDto postsSaveDto = PostsSaveDto.of(boards.getId(), "title" + i, "content" + i);
            postsService.savePosts(postsSaveDto, members);
        }

        posts = Posts.builder()
                .title("testTitle")
                .writer(members.getName())
                .content("testContent")
                .boards(boards)
                .members(members)
                .build();
        postsRepository.save(posts);
    }

    @Test
    @DisplayName("회원은 게시글을 작성할 수 있다.")
    public void 게시글_작성() throws Exception {
        //given
        PostsSaveDto postsSaveDto = PostsSaveDto.of(boards.getId(), "testTitle", "testContent");

        //when
        Long savePosts = postsService.savePosts(postsSaveDto, members);

        //then
        PostsInfoDto postsInfoDto = postsService.findPostsById(savePosts);
        assertThat(savePosts).isNotNull().isGreaterThan(0);
        assertThat(postsInfoDto.getTitle()).isEqualTo(postsSaveDto.getTitle());
        assertThat(postsInfoDto.getContent()).isEqualTo(postsSaveDto.getContent());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 5",  // 첫 번째 페이지, 5개 게시글
            "1, 5",  // 두 번째 페이지, 5개 게시글
            "0, 10", // 첫 번째 페이지, 10개 게시글 (모두 가져오기)
            "11, 1"   // 열두 번째 페이지, 게시글 없음 (경계 테스트)
    })
    @DisplayName("게시판별 게시글 페이징 조회")
    public void 게시글_페이징_조회(int page, int size) {
        //given
        PostsPagingRequestDto requestDto = new PostsPagingRequestDto(boards.getId(), page, size);

        //when
        Page<PostsInfoDto> result = postsService.findAllPostsPagingByBoardId(requestDto);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(size);

        int allPosts = postsRepository.findAll().size();

        if (page == allPosts) {
            assertThat(result.getContent()).isEmpty();
        } else {
            assertThat(result.getContent()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("작성자가 직접 작성한 게시글을 수정할 수 있다")
    public void 게시글_수정() throws Exception {
        //given
        PostsUpdateDto postsUpdateDto = PostsUpdateDto.of(posts.getId(), "updateTitle", "updateContent",
                members.getName());

        //when
        postsService.updatePostsById(postsUpdateDto);
        em.flush();
        em.clear();

        //then
        PostsInfoDto postsInfoDto = postsService.findPostsById(posts.getId());
        assertThat(postsInfoDto.getTitle()).isEqualTo(postsUpdateDto.getTitle());
        assertThat(postsInfoDto.getContent()).isEqualTo(postsUpdateDto.getContent());
        assertThat(postsInfoDto.getWriter()).isEqualTo(postsUpdateDto.getWriter());
    }

    @Test
    @DisplayName("작성자가 직접 작성한 게시글을 삭제할 수 있다")
    public void 게시글_삭제() throws Exception {
        //given
        PostsDeleteDto postsDeleteDto = PostsDeleteDto.of(posts.getId(), members.getName());

        //when
        postsService.deletePostsById(postsDeleteDto);
        em.flush();
        em.clear();

        //then
        assertThatThrownBy(() -> postsService.findPostsById(posts.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
