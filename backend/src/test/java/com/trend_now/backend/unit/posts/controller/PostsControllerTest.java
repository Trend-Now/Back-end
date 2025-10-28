package com.trend_now.backend.unit.posts.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trend_now.backend.board.application.BoardService;
import com.trend_now.backend.config.auth.CustomUserDetails;
import com.trend_now.backend.config.auth.CustomUserDetailsService;
import com.trend_now.backend.config.auth.JwtTokenFilter;
import com.trend_now.backend.config.auth.JwtTokenProvider;
import com.trend_now.backend.member.application.MemberRedisService;
import com.trend_now.backend.member.domain.Members;
import com.trend_now.backend.member.domain.Provider;
import com.trend_now.backend.post.application.PostsService;
import com.trend_now.backend.post.dto.PostsSaveDto;
import com.trend_now.backend.post.presentation.PostsController;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PostsController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PostsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostsService postsService;

    @MockitoBean
    private BoardService boardService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private MemberRedisService memberRedisService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtTokenFilter jwtTokenFilter;

    @BeforeEach
    public void setup() {
        Members mockMember = Members.builder()
                .id(1L)
                .name("testUser")
                .email("test@example.com")
                .snsId("testSnsId")
                .provider(Provider.TEST)
                .build();

        // SecurityContextHolder에 Mock Member 설정
        CustomUserDetails customUserDetails = new CustomUserDetails(mockMember);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("제목이 100자 이하면 게시글 저장에 성공한다")
    public void savePosts_TitleWithin100Characters_Success() throws Exception {
        // given
        Long boardId = 1L;
        String validTitle = "a".repeat(100);
        String validContent = "{\"ops\":[{\"insert\":\"테스트 내용\\n\"}]}";
        List<Long> imageIds = new ArrayList<>();

        PostsSaveDto postsSaveDto = PostsSaveDto.of(validTitle, validContent, imageIds);

        // when & then
        mockMvc.perform(post("/api/v1/boards/{boardId}/posts", boardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postsSaveDto)))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @DisplayName("내용이 10,000자 이하면 게시글 저장에 성공한다")
    public void savePosts_ContentWithin10000Characters_Success() throws Exception {
        // given
        Long boardId = 1L;
        String validTitle = "테스트 제목";
        String content = "a".repeat(10000);
        String validContent = String.format("{\"ops\":[{\"insert\":\"%s\"}]}", content);
        List<Long> imageIds = new ArrayList<>();

        PostsSaveDto postsSaveDto = PostsSaveDto.of(validTitle, validContent, imageIds);

        // when & then
        mockMvc.perform(post("/api/v1/boards/{boardId}/posts", boardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postsSaveDto)))
            .andExpect(status().isCreated())
            .andDo(print());
    }

    @Test
    @DisplayName("제목이 100자를 초과하면 게시글 저장에 실패한다")
    public void savePosts_TitleExceeds100Characters_Fails() throws Exception {
        // given
        Long boardId = 1L;
        String titleOver100 = "a".repeat(101);
        String validContent = "{\"ops\":[{\"insert\":\"테스트 내용\\n\"}]}";
        List<Long> imageIds = new ArrayList<>();

        PostsSaveDto postsSaveDto = PostsSaveDto.of(titleOver100, validContent, imageIds);

        // when & then
        mockMvc.perform(post("/api/v1/boards/{boardId}/posts", boardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postsSaveDto)))
            .andExpect(status().isBadRequest())
            .andDo(print());
    }

    @Test
    @DisplayName("내용이 10,000자를 초과하면 게시글 저장에 실패한다")
    public void savePosts_ContentExceeds10000Characters_Fails() throws Exception {
        // given
        Long boardId = 1L;
        String validTitle = "테스트 제목";
        String content = "a".repeat(10001);
        String contentOver10000 = String.format("{\"ops\":[{\"insert\":\"%s\"}]}", content);
        List<Long> imageIds = new ArrayList<>();

        PostsSaveDto postsSaveDto = PostsSaveDto.of(validTitle, contentOver10000, imageIds);

        // when & then
        mockMvc.perform(post("/api/v1/boards/{boardId}/posts", boardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postsSaveDto)))
            .andExpect(status().isBadRequest())
            .andDo(print());
    }

    @Test
    @DisplayName("제목이 비어있으면 게시글 저장에 실패한다")
    public void savePosts_EmptyTitle_Fails() throws Exception {
        // given
        Long boardId = 1L;
        String emptyTitle = "";
        String validContent = "{\"ops\":[{\"insert\":\"테스트 내용\\n\"}]}";
        List<Long> imageIds = new ArrayList<>();

        PostsSaveDto postsSaveDto = PostsSaveDto.of(emptyTitle, validContent, imageIds);

        // when & then
        mockMvc.perform(post("/api/v1/boards/{boardId}/posts", boardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postsSaveDto)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("내용이 비어있으면 게시글 저장에 실패한다")
    public void savePosts_EmptyContent_Fails() throws Exception {
        // given
        Long boardId = 1L;
        String validTitle = "테스트 제목";
        String emptyContent = "";
        List<Long> imageIds = new ArrayList<>();

        PostsSaveDto postsSaveDto = PostsSaveDto.of(validTitle, emptyContent, imageIds);

        // when & then
        mockMvc.perform(post("/api/v1/boards/{boardId}/posts", boardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postsSaveDto)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }
}