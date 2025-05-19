package com.trend_now.backend.board.cache;

import com.trend_now.backend.board.domain.BoardCategory;
import com.trend_now.backend.board.domain.Boards;
import com.trend_now.backend.board.repository.BoardRepository;
import com.trend_now.backend.board.util.BoardServiceUtil;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealTimeBoardCache {

    private final BoardRepository boardRepository;
    private final BoardServiceUtil boardServiceUtil;

    @Getter
    private final List<BoardCacheEntry> boardCacheEntryList = new ArrayList<>();
    @Getter
    private List<Boards> fixedBoardList;
    @Getter
    private List<Long> boardCacheIdList = new ArrayList<>();


    public void setBoardInfo(Set<String> boardRank) {
        boardCacheEntryList.clear();
        boardRank.forEach(keyword -> {
            Long boardId = Long.parseLong(keyword.split(":")[1]);
            boardCacheIdList.add(boardId);
            String boardName = keyword.split(":")[0];
            String trimmedName = boardName.replaceAll(" ", "");
            String disassembledBoardName = boardServiceUtil.disassembleText(trimmedName);
            BoardCacheEntry boardCacheEntry = BoardCacheEntry.builder()
                .boardId(boardId)
                .boardName(boardName)
                .disassembledBoardName(disassembledBoardName)
                .build();
            boardCacheEntryList.add(boardCacheEntry);
        });
    }

    // 고정 게시판 초기화
    @PostConstruct
    public void initFixedBoard() {
        fixedBoardList = boardRepository.findByNameLikeAndBoardCategory("%", BoardCategory.FIXED);
    }
}
