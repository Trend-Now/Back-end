package com.trend_now.backend.board.application;

import static com.trend_now.backend.board.util.BoardServiceUtil.BOARD_RANK_KEY;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.util.BoardServiceUtil;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardServiceUtil boardServiceUtil;
    private final RedisTemplate<String, String> redisTemplate;

    public static List<Boards> fixedBoardList;

    // 고정 게시판 초기화
    @PostConstruct
    public void initFixedBoard() {
        fixedBoardList = boardRepository.findByNameLikeAndBoardCategory("%", BoardCategory.FIXED);
    }

    @Transactional
    public Long saveBoardIfNotExists(BoardSaveDto boardSaveDto) {
        Boards board = boardRepository.findByName(boardSaveDto.getName())
            .orElseGet(() -> boardRepository.save(
                    Boards.builder()
                        .name(boardSaveDto.getName())
                        .boardCategory(boardSaveDto.getBoardCategory())
                        .build()
                )
            );
        return board.getId();
    }


    @Transactional
    public void updateBoardIsDeleted(BoardSaveDto boardSaveDto, boolean isInRedis) {
        // 요구사항을 기반으로 Redis에 있는 게시판 데이터는 DB에도 존재해야 한다.
        Boards findBoards = boardRepository.findByName(boardSaveDto.getName())
            .orElseThrow(
                () -> new IllegalStateException("해당 게시판이 존재하지 않습니다: " + boardSaveDto.getName())
            );

        if (isInRedis) {
            if (findBoards.isDeleted()) {
                findBoards.changeDeleted();
            }
        } else {
            if (!findBoards.isDeleted()) {
                findBoards.changeDeleted();
            }
        }
    }

    public List<BoardInfoDto> findBoardsByPrefix(String prefix) {
        // 공백 제거
        String trimmedPrefix = prefix.replaceAll(" ", "");
        // 현재 Redis에 저장된 실시간 인기 검색어 조회 (게시판 이름:게시글 ID) 형태로 되어 있음
        Set<String> realTimeRank = redisTemplate.opsForZSet().range(BOARD_RANK_KEY, 0, -1);
        // 입력된 prefix를 자모 분해
        String disassemblePrefix = boardServiceUtil.disassembleText(trimmedPrefix);

        List<BoardInfoDto> filteredBoards = new ArrayList<>();
        for (String realTimeKeyword : realTimeRank) {
            // 게시판 이름과 게시판 id 분리
            String[] splitKeyword = realTimeKeyword.split(":");
            String boardName = splitKeyword[0];
            Long boardId = Long.parseLong(splitKeyword[1]);
            // 게시판 이름을 자모 분해
            String disassembleBoardName = boardServiceUtil.disassembleText(boardName.replaceAll(" ", ""));
            // 게시판 이름이 prefix로 시작하지 않으면 continue
            if (!disassembleBoardName.startsWith(disassemblePrefix)) continue;

            filteredBoards.add(BoardInfoDto.builder()
                .boardName(boardName)
                .boardId(boardId)
                .build());
        }

        // 고정 게시판 조회
        List<BoardInfoDto> fixedBoardList = BoardService.fixedBoardList.stream().map(funnyBoard ->
                BoardInfoDto.builder()
                    .boardId(funnyBoard.getId())
                    .boardName(funnyBoard.getName())
                    .build())
            .toList();

        // 실시간 게시판과 고정 게시판 결합
        List<BoardInfoDto> result = new ArrayList<>(filteredBoards);
        result.addAll(fixedBoardList);

        return result;
    }
}
