package com.trend_now.backend.comment.repository;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.comment.data.dto.FindAllCommentsDto;
import com.trend_now.backend.comment.domain.Comments;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.yml")
class CommentsRepositoryTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CommentsRepository commentsRepository;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Posts testPost;
    private Members testMembers;
    private Boards testBoards;

    @BeforeEach
    public void setUp() {
        testMembers = memberRepository.save(Members.builder()
                .name("testUser")
                .email("testEmail")
                .snsId("testSnsId")
                .provider(Provider.TEST)
                .build());

        testBoards = boardRepository.save(Boards.builder()
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
    @DisplayName("findByPostsIdOrderByCreatedAtDesc가 FindAllCommentsDto로 프로젝션되는지 검증")
    public void 게시글_모든_댓글_조회_결과_확인() {
        // Given
        // 댓글 2개를 동일 게시글에 저장
        Comments comment1 = Comments.builder()
                .content("첫 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        Comments comment2 = Comments.builder()
                .content("두 번째 댓글입니다.")
                .posts(testPost)
                .members(testMembers)
                .build();

        commentsRepository.save(comment1);
        commentsRepository.save(comment2);

        em.flush();
        em.clear();

        // When
        // 해당 게시글에 댓글을 조회
        List<FindAllCommentsDto> actualDtos = commentsRepository.findByPostsIdOrderByCreatedAtDesc(testPost.getId());

        // findAll()로 조회한 후 수동으로 변환
        List<Comments> allComments = commentsRepository.findAll().stream()
                .filter(comment -> comment.getPosts().getId().equals(testPost.getId()))
                .collect(Collectors.toList());

        // 생성일 기준 내림차순 정렬
        allComments.sort(Comparator.comparing(Comments::getCreatedAt).reversed());

        List<FindAllCommentsDto> expectedDtos = allComments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        // Then
        // commentsRepository.findByPostsIdOrderByCreatedAtDesc()로 프로젝션한 List<FindAllCommentsDto>와
        // commentsRepository.findAll() 결과를 수동 변환한 것이 같은지 확인
        assertThat(actualDtos).hasSize(2);
        assertThat(expectedDtos).hasSize(2);

        // 내용과 ID 기준으로 비교
        for (int i = 0; i < actualDtos.size(); i++) {
            FindAllCommentsDto actual = actualDtos.get(i);
            FindAllCommentsDto expected = expectedDtos.get(i);

            System.out.println(i + "번째 actualDtos > " + actual.toString());
            System.out.println(i + "번째 allComments > " + expected.toString());

            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getContent()).isEqualTo(expected.getContent());
            assertThat(actual.getCreatedAt()).isEqualToIgnoringNanos(expected.getCreatedAt());
        }

        System.out.println("actualDtos > " + actualDtos.toString());
        System.out.println("allComments > " + allComments.toString());
    }

    private FindAllCommentsDto convertToDto(Comments comment) {
        return FindAllCommentsDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}