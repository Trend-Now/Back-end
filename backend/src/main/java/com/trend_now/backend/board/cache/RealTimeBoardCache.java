package com.trend_now.backend.board.cache;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.util.BoardServiceUtil;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealTimeBoardCache {

    private final BoardRepository boardRepository;
    private final BoardServiceUtil boardServiceUtil;

    @Getter
    private List<BoardCacheEntry> boardCacheEntryList;
    @Getter
    private List<BoardCacheEntry> fixedBoardCacheList;
    @Getter
    private List<Long> boardCacheIdList;


    @Async
    public void setBoardInfo(Set<String> boardRank) {
        boardCacheIdList = boardRank.stream().map(
            keyword -> Long.parseLong(keyword.split(":")[1])
        ).toList();
        List<Boards> boardsList = boardRepository.findByIdIn(boardCacheIdList);
        boardCacheEntryList = boardsList.stream().map(
            board -> BoardCacheEntry.builder()
                .boardId(board.getId())
                .boardName(board.getName())
                .disassembledBoardName(boardServiceUtil.disassembleText(board.getName()))
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build()
        ).toList();
    }

    // 고정 게시판 초기화
    @PostConstruct
    public void initFixedBoard() {
        List<Boards> fixedBoardList = boardRepository.findByNameLikeAndBoardCategory(
            "%", BoardCategory.FIXED);
        this.fixedBoardCacheList = fixedBoardList.stream()
            .map(fixedBoard -> BoardCacheEntry.builder()
                .boardId(fixedBoard.getId())
                .boardName(fixedBoard.getName())
                .disassembledBoardName(boardServiceUtil.disassembleText(fixedBoard.getName()))
                .build())
            .toList();
    }
}
