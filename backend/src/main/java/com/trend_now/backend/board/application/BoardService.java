package com.trend_now.backend.board.application;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.dto.BoardInfoDto;
import com.trend_now.backend.board.dto.BoardSaveDto;
import com.trend_now.backend.board.dto.FixedBoardSaveDto;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.util.BoardServiceUtil;
import com.trend_now.backend.board.cache.BoardCacheEntry;
import com.trend_now.backend.board.cache.RealTimeBoardCache;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardServiceUtil boardServiceUtil;
    private final RealTimeBoardCache realTimeBoardCache;

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
        // 입력된 prefix를 자모 분해
        String disassemblePrefix = boardServiceUtil.disassembleText(trimmedPrefix);
        // 캐싱해놓은 실시간 인기 검색어 리스트 조회
        List<BoardCacheEntry> boardCacheEntryList = realTimeBoardCache.getBoardCacheEntryList();

        List<BoardInfoDto> filteredBoards = new ArrayList<>();
        for (BoardCacheEntry boardEntry : boardCacheEntryList) {
            // 게시판 이름이 prefix로 시작하지 않으면 continue
            String disassembleBoardName = boardEntry.getDisassembledBoardName();
            if (!disassembleBoardName.startsWith(disassemblePrefix)) {
                continue;
            }

            filteredBoards.add(BoardInfoDto.builder()
                .boardName(boardEntry.getBoardName())
                .boardId(boardEntry.getBoardId())
                .build());
        }

        // 고정 게시판 조회
        List<BoardInfoDto> fixedBoardList = realTimeBoardCache.getFixedBoardCacheList().stream()
            .filter(fixBoard -> fixBoard.getDisassembledBoardName().startsWith(disassemblePrefix))
            .map(fixedBoard ->
                BoardInfoDto.builder()
                    .boardId(fixedBoard.getBoardId())
                    .boardName(fixedBoard.getBoardName())
                    .build())
            .toList();

        // 실시간 게시판과 고정 게시판 결합
        List<BoardInfoDto> result = new ArrayList<>(filteredBoards);
        result.addAll(fixedBoardList);

        return result;
    }

    @Transactional
    public void addFixedBoard(FixedBoardSaveDto fixedBoardSaveDto) {
        boardRepository.save(
            Boards.builder()
                .name(fixedBoardSaveDto.getBoardName())
                .boardCategory(BoardCategory.FIXED)
                .build());
        realTimeBoardCache.initFixedBoard();
    }
}
